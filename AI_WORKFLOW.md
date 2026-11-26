# AI_WORKFLOW — Registro de ingeniería con IA

Registro de cómo se construyó **Slime Rancher Tracker** con un agente de codificación: qué se le pidió, qué propuso, qué se corrigió y qué decisiones de arquitectura se tomaron.

## 1. Herramienta y metodología

**Herramienta:** Claude Code (agente de Anthropic en la terminal) en lugar de Junie, que es el que nombra el enunciado.

**Metodología:**
- **`CLAUDE.md` en la raíz** (el "Prompt 0"): stack fijo, paquetes, dependencias entre capas, las 13 reglas obligatorias (cero credenciales en el código, `PreparedStatement`, try-with-resources, `GestorTransacciones`, excepciones propias, códigos HTTP, no hacer commits…). Claude Code lo carga en cada conversación.
- **Trabajo por fases** con los prompts de [`.docs/PROMPTS_CLAUDE.md`](.docs/PROMPTS_CLAUDE.md): esqueleto y Docker → DAO → servicios → API → interfaz → tests → auditoría → documentación.
- **Modo plan:** antes de tocar nada, el agente dice qué supone y enseña el plan; el alumno lo aprueba o lo corrige.
- **Skills:**
  - `karpathy-guidelines` en todo el código: explicitar las suposiciones, hacer lo mínimo, cambios quirúrgicos y criterios de éxito verificables.
  - `charles-unslop` en la interfaz: la dirección visual ya estaba aprobada en `.docs/diseno/`, así que la skill se usó para respetarla y auditar, no para inventar otra.
- **Verificación por el propio agente (regla 12)** antes de dar algo por terminado:
  - `mvn verify` en un contenedor `maven:3.9-eclipse-temurin-21`;
  - `docker compose up --build`;
  - `curl` y un ejecutor de `docs/api.http`;
  - un recorrido de la web con Chrome (Playwright) para comprobar capturas, errores de consola y flujo completo.
- **Commits:** la regla 13 decía que los hacía el alumno, pero al terminar las fases el alumno pidió al agente que hiciera un commit por fase (Conventional Commits, en español). Los separó por archivos y comprobó que cada uno compila solo.

**Cómo se trabajó de verdad:**
- La **fase 1** se hizo con plan aprobado.
- De la **fase 2 a la 8**, el alumno pidió al agente que *"continuara hasta el final sin parar"*, así que no hubo aprobación de plan entre fases. Las decisiones que el agente tomó solo están en la sección 3 para que el alumno las revise.
- **No se rehízo ninguna fase.** Lo que sí se rehízo fue el proyecto entero: el primer intento, con un prompt corto, se borró (ver [Conclusiones](#5-conclusiones)). Este es el segundo, hecho con `CLAUDE.md` y los prompts por fases.

## 2. Por fase

### Fase 1 — Esqueleto, Docker y base de datos

- **Prompt:** [FASE 1](.docs/PROMPTS_CLAUDE.md#fase-1--esqueleto-docker-y-base-de-datos): `pom.xml`, `db/init.sql` con 8 tablas y los datos de `DATOS_DEMO.md`, Dockerfile multi-stage, `docker-compose.yml` (db, app, adminer), `.env.example`, `ConfigBD`, Hikari y `GET /api/salud`.
- **Plan propuesto:**
  - adaptar el `pom.xml` de IntelliJ, que estaba en Java 27 → `release 21`;
  - `groupId com.slimetracker`;
  - `init.sql` sin `CREATE DATABASE`, para no escribir el nombre de la BD;
  - hashes BCrypt generados con la misma librería que usa la app;
  - marcar con `-- REVISAR:` los datos del juego que no cuadran.
- **Preguntas del agente y qué se decidió:**
  1. `/api/salud` → se hace como **recurso → servicio → DAO**, para no saltarse las capas desde el primer día.
  2. No había `mvn` instalado → se compila con **Maven dentro de Docker** (sin Maven Wrapper en el repo).
- **Errores que encontró el agente al probar y que arregló:**
  - `CHECK (zona_requisito_id <> id)` → MariaDB da `ERROR 1901`: no admite columnas `AUTO_INCREMENT` en un `CHECK`. Se cambió por dos triggers.
  - HikariCP 6.2.1 trae `slf4j-api` 1.7 y `slf4j-simple` 2.x no la reconoce: **todos los logs se perdían**. Se fijó `slf4j-api` 2.0.16.
  - Con el usuario no root, Tomcat no podía crear `conf/Catalina/localhost` (`SEVERE` al arrancar). Se añadió al `chown` del Dockerfile.
  - Si faltaba una variable de entorno, el mensaje claro solo salía en un archivo de log *dentro* del contenedor. Ahora el listener lo escribe en `docker compose logs app`.
- **Errores corregidos a mano:** ninguno. Los fallos de arriba los encontró y arregló el agente al probar.
- **Datos marcados como `REVISAR` en `init.sql`** (Ancient Ruins, The Wilds, y Gold/Lucky/Phosphor, que salen en varias zonas): se mantienen los de `DATOS_DEMO.md`. El modelo da a cada slime **una sola zona**, la principal, y una sola zona requisito a cada zona. Es una simplificación a propósito: si un slime estuviera en varias zonas, haría falta otra tabla N:M solo para eso.
- **Decisiones de arquitectura del alumno** (fijadas en el prompt antes de programar):
  - Las 8 tablas con sus claves, `UNIQUE`, `CHECK` y acciones `ON DELETE`: `CASCADE` desde usuario y partida, y `RESTRICT` hacia el catálogo, para que no se pueda borrar una zona, un slime o un logro que alguien tiene marcado.
  - El catálogo (zonas, slimes, logros) es de solo lectura y se carga desde `init.sql`. El usuario solo escribe en las tres tablas puente.
  - En Docker: la BD sin puertos publicados, en una red `backend` interna, con healthcheck y `service_healthy`; volumen nombrado; Tomcat con un usuario no root.

### Fase 2 — Modelo, DTOs y DAO (JDBC)

- **Prompt:** [FASE 2](.docs/PROMPTS_CLAUDE.md#fase-2--modelo-dtos-y-dao-jdbc): entidades, `TraductorSqlException`, `GestorTransacciones`, DAOs, DTOs y mapeadores.
- **Qué generó:**
  - Entidades como `record` (una por tabla) y proyecciones de lectura (`ZonaConEstado`, `SlimeConEstado`, `LogroConEstado`) para los `LEFT JOIN`.
  - Siete DAO JDBC; cada `PreparedStatement` y cada `ResultSet` van en su propio try-with-resources.
  - `listarConEstado` es **una sola consulta** con `LEFT JOIN`, sin el problema N+1.
- **Decisiones del agente** (para revisar):
  - El traductor reconoce el **código de MariaDB** (1062/1451/1452) **y** el **SQLState estándar** (23505/23503/23506), para que los tests con H2 den las mismas excepciones.
  - Fechas como `Instant`: el JSON sale en ISO-8601 con `Z` y el navegador las enseña en hora local.
  - Los DTOs no usan los `enum` de `modelo` (dieta, rareza y categoría van como `String`), para que nada de `modelo` llegue a `recurso`.
  - El "desglose de slimes por zona" sale de `ZonaDao.listarConEstado`, que ya trae los recuentos de cada zona, y no de una consulta aparte en `ProgresoDao`.
  - `ZonaDao.listarDesbloqueadas` no se creó, porque `listarConEstado` la cubre.
  - Para el mensaje de R5 hacían falta los nombres de los slimes, así que `contarDescubiertosEnZona` pasó a `listarDescubiertosEnZona`, que los devuelve.
  - El filtro opcional del catálogo de slimes se construye con **fragmentos fijos** (`"s.zona_id = ?"`); los valores van siempre como parámetros.
- **Errores corregidos a mano:** ninguno. Se revisó lo que pedía el prompt: que no hubiera `" +` con datos en el SQL, un try-with-resources por `ResultSet` y `listarConEstado` en una sola consulta.
- **Decisiones de arquitectura del alumno:**
  - Los DAO reciben la `Connection` como primer parámetro. Así varios DAO comparten la transacción que abre el servicio con `GestorTransacciones`.
  - Las `SQLException` se traducen **en el DAO** a excepciones propias unchecked, para que el servicio no dependa de JDBC.
  - Entidades y DTOs separados, con mapeadores en medio: las entidades no salen de la capa de servicio.

### Fase 3 — Servicios y reglas de negocio

- **Prompt:** [FASE 3](.docs/PROMPTS_CLAUDE.md#fase-3--servicios-y-reglas-de-negocio): R1–R10 en seis servicios.
- **Qué generó:**
  - Seis servicios que reciben DAOs y `GestorTransacciones` por constructor.
  - Dos clases de apoyo compartidas, para que ningún servicio dependa de otro:
    - `PropiedadPartida` hace la comprobación 404/403 común a todos los métodos de partida;
    - `NuevaPartida` crea una partida con la zona inicial, para R1 y R3.
- **Decisiones del agente:**
  - **R10:** el global es la media de las tres **fracciones exactas** y no de los porcentajes ya redondeados. (71 + 61 + 40) / 3 daría 57; las fracciones dan 57,51 → 58, que es lo que pide el demo.
  - **R2:** si el usuario no existe, se compara igualmente contra un hash ficticio, para no revelar por el tiempo de respuesta qué usuarios existen.
  - El registro pide `confirmacion`, porque el diseño enseña el error "Las contraseñas no coinciden." como un error de campo más.
  - El 409 de nombre repetido se detecta con la clave única (`EntidadDuplicadaException`) y no con una consulta previa, para evitar carreras.
- **Errores corregidos a mano:** ninguno. Se revisó lo que pedía el prompt: el 403 está en todos los métodos de partida (los tests lo comprueban), R7 es una sola transacción y ningún mensaje de error enseña SQL.
- **Decisiones de arquitectura del alumno:**
  - Las reglas R1–R10 y su código HTTP las escribió el alumno en el prompt.
  - Los servicios reciben sus dependencias por constructor y `ContextoApp` los crea a mano, sin framework de inyección. Así se pueden probar con Mockito.
  - Validación a mano en el servicio (sin Bean Validation), devolviendo **todos** los errores de campo a la vez.

### Fase 4 — Autenticación y API REST

- **Prompt:** [FASE 4](.docs/PROMPTS_CLAUDE.md#fase-4--autenticación-y-api-rest).
- **Qué generó:**
  - Filtro de sesión.
  - `changeSessionId()` al entrar.
  - Cookie HttpOnly + SameSite=Strict.
  - Los endpoints.
  - `docs/api.http` con 65 peticiones y su código esperado. Todas pasan contra `docker compose up`.
- **Decisiones del agente:**
  - Un solo `MapeadorErrores implements ExceptionMapper<Throwable>` con un `switch` por tipo (Java 21) en vez de once clases casi iguales.
  - Jackson registrado **sin sus ExceptionMappers**, para que el JSON mal formado también salga con el formato de error común (si no, Jersey devuelve texto plano).
  - Los ids de ruta se reciben como `String` y se validan en el recurso. Con `long`, Jersey responde 404 a `/partidas/abc` en vez del 400 que pide el prompt.
  - `connectionTimeout` de Hikari a 5 s (por defecto son 30): si la BD cae, la web avisa antes del timeout de 10 s del frontend.
  - Al registrarse, la sesión queda iniciada.
- **Errores corregidos a mano:** ninguno. Lo que pedía el prompt revisar lo comprueba `docs/api.http`: 403 con la partida de `vecina`, 400 con JSON roto y 404 con un id que no existe.
- **Decisiones de arquitectura del alumno:**
  - Sesión con `HttpSession` en vez de tokens. En la sesión solo se guardan el id y el nombre del usuario.
  - Protección de la sesión: `changeSessionId()` al entrar y cookie `HttpOnly` + `SameSite=Strict`.
  - La capa de control valida la forma de la petición (ids, cuerpo, JSON) y el servicio valida las reglas de negocio.
  - Un formato de error JSON igual para todos los códigos, y los 500 sin detalles para el cliente.

### Fase 5 — Interfaz web (charles-unslop)

- **Prompt:** [FASE 5a/5b/5c](.docs/PROMPTS_CLAUDE.md#fase-5--interfaz-web-con-charles-unslop).
- **Qué generó:** 7 páginas, `css/estilos.css` a partir del CSS aprobado y módulos JS (`api.js`, `ui.js`, `estado.js`, `fechas.js`, `cabecera.js` y uno por página).
- **Comparación con el diseño (5c):** con los datos del demo se capturaron todas las páginas a 1280 px, el panel a 390 px y varias en oscuro, y se compararon con `.docs/diseno/capturas/`:
  - Panel, Slimepedia, Zonas (aviso 409), Mis partidas (409 + confirmación), Entrar (401, oscuro) y Crear cuenta (400 con tres errores) coinciden.
  - **Diferencia encontrada y corregida:** en Logros, el enlace "Añadir nota" que añadió el agente metía líneas que el diseño no tiene. Ahora, con ratón, solo aparece al pasar por la fila o al llegar con el teclado, sin mover nada; en pantallas táctiles se ve siempre.
  - **Diferencia que se mantiene:** en Crear cuenta, el primer campo con error tiene el anillo de foco, porque el foco se lleva ahí a propósito por accesibilidad.
- **Desviaciones del diseño que decidió el agente** (para que el alumno las apruebe o las quite):
  1. **Fuentes locales:** el diseño las cargaba de Google Fonts, pero `CLAUDE.md` prohíbe los CDN. Se sirven desde `fuentes/` (Nunito y Fredoka son OFL).
  2. **Colores a tokens:** los colores que el CSS de referencia tenía fuera de `:root` (`#FFFFFF` de los botones y `.c-*` de los slimes) pasan a tokens de `:root`, con los mismos valores.
  3. **Últimos cambios:** el panel enseña 5, como la captura, aunque la API devuelve 10 (R10).
  4. **Edición en línea:** "Renombrar" y "Añadir/Editar nota" en logros, porque el diseño no enseña cómo se editan.
  5. **Placeholder** "Nombre de la nueva partida" en el campo de Mis partidas.
  6. **Textos nuevos** para estados que el diseño no muestra: cargando, listas vacías, "Ir a Zonas" en el detalle de un slime de zona bloqueada y "No hay nada pendiente…" en *Ahora puedes*.
- **Errores que encontró el agente al probar:** dos acciones en el mismo segundo salían desordenadas en "Últimos cambios", porque las fechas iban a segundos y se desempataba por nombre. Las tres fechas de progreso pasaron a `TIMESTAMP(6)`.
- **Comprobado en Chrome:**
  - El flujo completo registro → partida → zona → slime → logro → panel (25 comprobaciones).
  - `<b>hola</b>` como nombre se ve como texto, sin XSS.
  - El tema se mantiene al cambiar de página y al recargar.
  - Con la BD parada sale un mensaje claro.
  - A 375 px no hay scroll horizontal.
  - En consola solo aparecen los avisos de red de Chrome por los 4xx provocados a propósito.

### Fase 6 — Batería de pruebas

- **Prompt:** [FASE 6](.docs/PROMPTS_CLAUDE.md#fase-6--batería-de-pruebas).
- **Qué generó:** 255 tests con `@DisplayName` en español, `@Nested` y `@ParameterizedTest`. `mvn clean verify` en verde.
- **Cobertura de líneas:** servicio **97,8 %**, dao **99,3 %**, total 84,5 %.
- **Diferencias de `schema-h2.sql` respecto a `init.sql`** (están comentadas en el archivo):
  - sin `ENGINE` ni `CHARSET`;
  - `color` sin `ascii_bin`;
  - **sin los triggers** de zona requisito, porque en H2 un trigger necesita una clase Java;
  - índices con `CREATE INDEX` aparte;
  - `INTERVAL '23' DAY` en lugar de `INTERVAL 23 DAY`.
- **Bug real que destaparon los tests:** `descubrirVarios` usaba `ids.contains(null)`, que lanza `NullPointerException` con listas inmutables (`List.of`). Se cambió por `stream().anyMatch(Objects::isNull)`.
- **Comprobación de que los tests sirven:** al quitar a propósito la comprobación del 403 en `PropiedadPartida`, fallan 12 tests.
- **Captura de cobertura:** [`docs/img/cobertura.png`](docs/img/cobertura.png) (85 % de instrucciones y 93 % de ramas en total).
- **Comprobación antes de entregar:** `mvn verify` desde cero da **255 tests, 0 fallos y 0 errores**. Las trazas de pila que salen en la consola son los 500 que algunos tests provocan a propósito, y que el mapeador registra en el log.

### Fase 7 — Auditoría de código

Hecha con `karpathy-guidelines` en modo revisor: medición automática (SQL concatenado, imports sin usar, métodos de más de 40 líneas, entidades fuera de capa, código sin usar) más una revisión a mano de seguridad, códigos HTTP y Docker.

| Archivo:línea | Problema | Gravedad | Propuesta | ¿Aplicado? |
|---|---|---|---|---|
| `excepcion/MapeadorErrores.java:56` | Los 4xx de Jersey (415, 406) llegaban con su texto en inglés ("HTTP 415 Unsupported Media Type"), en contra de la regla 10 | Media | Mensajes propios en español | **Sí** |
| `dao/LogroDao.java` · `LogroDaoJdbc` | `buscarPorId` no lo usaba ningún servicio: código muerto | Baja | Quitarlo | **Sí** |
| `modelo/PartidaZona`, `PartidaSlime`, `PartidaLogro` | Records que no usa el código de producción | Baja | Quitarlos | **No:** el prompt de la fase 2 pide una entidad por tabla, y documentan las tres tablas puente |
| `dao/jdbc/SlimeDaoJdbc.java:58` | SQL montado con `+` | Ninguna (falso positivo) | — | **No:** solo se añaden fragmentos fijos; los valores van con `?` |
| `seguridad/FiltroAutenticacion.java` | `/api/salud/` (con barra final) pide sesión | Baja | Normalizar la barra final | **No:** ningún cliente la usa |
| `servicio/AutenticacionServicio.java` | BCrypt solo usa los primeros 72 bytes de la contraseña; una más larga se trunca sin avisar | Baja | Máximo de 72 caracteres en R1 | **No:** cambia R1, lo decide el alumno |
| Login | Sin límite de intentos (fuerza bruta) | Media | Bloqueo temporal tras N fallos | **No:** fuera del alcance de la práctica |
| `WEB-INF/web.xml` | Cookie sin `Secure` | Baja en local | `<secure>true</secure>` detrás de HTTPS | **No:** en local se sirve por HTTP; con `Secure` la sesión no funcionaría |
| `docker-compose.yml` | `adminer` sin versión fija (`latest`) | Baja | `adminer:4.8.1` | **No:** la etiqueta la fija el prompt de la fase 1 |
| Verificado sin problemas | `PreparedStatement` en todo; try-with-resources en todo; rollback en `GestorTransacciones`; entidades solo en dao, servicio y mapeador; ningún recurso importa `dao` ni `modelo`; propiedad comprobada en los 16 endpoints de partida (403 probado con `vecina`); sin hash en DTOs ni logs; sin `innerHTML` con datos; cambio de id de sesión al entrar; cookie HttpOnly + SameSite=Strict; ningún método de más de 40 líneas; ningún import sin usar; contenedor como usuario no root; healthcheck + `service_healthy` | — | — | — |

**Qué se decidió:** el agente aplicó los dos arreglos marcados como "Sí" sin preguntar, porque el alumno le había pedido no parar. El alumno los da por buenos y acepta los descartes con los motivos de la tabla. En concreto, el límite de 72 caracteres de BCrypt no se añade porque cambiaría R1, y el límite de intentos de login queda fuera del alcance de la práctica.

### Fase 8 — Documentación

- **Prompt:** [FASE 8](.docs/PROMPTS_CLAUDE.md#fase-8--documentación).
- **Qué generó:** este archivo y el `README.md`. El README incluye arquitectura, diagrama ER y contenedores en Mermaid (comprobado que se renderizan), despliegue paso a paso, credenciales, persistencia, manual, API, reglas y tests.
- **Prueba desde un clon limpio:** se copiaron solo los archivos versionados en git, se hizo `cp .env.example .env` y `docker compose up --build -d`, como dice el README:
  - La BD arrancó *healthy* antes que la app y `GET /api/salud` dio 200.
  - Con `rancher` / `slime1234`, la Primera partida sale al 58 %, sin errores en la consola del navegador.
  - **Lo que falló:** el README enlazaba siete capturas de `docs/img/` que no existían. Se hicieron sobre ese despliegue limpio, con los datos del demo, y se añadieron.
  - El único cambio respecto al README fueron los puertos (8090/8091), porque la otra copia ya ocupaba el 8080.
- **Enlace al repositorio** al principio del README.

## 3. Decisiones que tomó el agente sin preguntar (para revisar)

Desde la fase 2 se trabajó sin parar entre fases, así que estas decisiones no las aprobó nadie. Están explicadas en cada fase:

1. **Triggers** en lugar del `CHECK (zona_requisito_id <> id)`, que MariaDB no admite.
2. **`TIMESTAMP(6)`** en las tres tablas de progreso.
3. **Fuentes servidas desde el WAR** en lugar de Google Fonts.
4. **Un único `ExceptionMapper`** con `switch`.
5. **Ids de ruta como `String`**, validados en el recurso.
6. **Registro con sesión iniciada** y campo `confirmacion` en la petición.
7. **Edición en línea** de nombres de partida y notas de logros, y el **placeholder** del campo de Mis partidas.
8. **5 cambios visibles en el panel** aunque la API devuelve 10.
9. **Hikari `connectionTimeout` a 5 s.**
10. **Datos de los slimes sin inventar:** `DATOS_DEMO.md` no da `comida_favorita`, `descripcion` ni `peligroso`, así que se quedan vacíos.

## 4. Diseño

- El diseño se hizo **antes de programar**: un wireframe de alta fidelidad con `charles-unslop`, que está en [`.docs/diseno/`](.docs/diseno/GUIA_VISUAL.md) (guía visual, capturas en claro, oscuro y móvil, HTML estático y datos de prueba).
- **Dirección:**
  - Tono blando, curioso y ordenado; el mundo es el agua y el cielo del Far, Far Range.
  - Un solo acento (rosa `#E0156E`).
  - La firma son las marcas de "hecho" con forma de **plort**.
  - El momento es la Slimepedia: los slimes sin descubrir son siluetas con "?" y al marcarlos aparecen con su color y su cara.
- La web real se comparó captura a captura con el diseño; las diferencias corregidas están en la fase 5.
- **Auditoría final de `charles-unslop` sobre el código:** sin gradientes, sin pesos 700+, sin mayúsculas espaciadas, sin colores fuera de `:root`, sin emojis ni CDN.

## 5. Conclusiones

### Qué pasó
El primer intento salió mal y tuve que borrarlo entero. El prompt era corto y casi no especificaba
nada, así que la IA decidió por su cuenta y el resultado tenía tres problemas graves:
- la arquitectura estaba mal planteada;
- el diseño de la web era muy malo;
- la autenticación de usuarios no funcionaba bien.

En el segundo intento cambié de enfoque. Antes de escribir código preparé un prompt largo y explícito,
dividido en fases, que incluía:
- el stack, la estructura de paquetes y las reglas de negocio;
- los datos de prueba;
- las vistas de la web ya diseñadas, para que la IA tuviera una referencia visual exacta.

Como todo estaba especificado, la IA no tuvo que inventarse nada y lo hizo como yo quería. La web
quedó mucho mejor, tardé bastante menos y no tuve que corregir errores.

### Qué he aprendido
- **El prompt y el contexto lo deciden casi todo.** Si no se lo especificas, la IA lo rellena con
  suposiciones. Darle requisitos claros, ejemplos y el diseño de las pantallas antes de empezar ahorra
  más tiempo del que cuesta prepararlos.
- **Las skills mejoran el resultado.** `karpathy-guidelines` le hizo explicar sus suposiciones y
  hacer cambios pequeños y comprobables. `charles-unslop` evitó que la interfaz tuviera el aspecto
  genérico de "hecho con IA".
- **Para programar funciona mejor desde la terminal (CLI) que desde la aplicación de chat.** Desde la
  CLI trabaja directamente sobre los archivos del proyecto y puede ejecutar Maven, Docker y los tests
  para comprobar lo que hace. Además, con el modo plan reviso cada fase antes de que toque nada.

### Qué haré en los próximos proyectos
Antes de pedir código dedicaré tiempo a preparar el contexto: requisitos, decisiones de diseño, datos
de ejemplo y un `CLAUDE.md` con las reglas del proyecto. También dividiré el trabajo en fases que pueda
revisar y usaré skills y la CLI desde el principio.
