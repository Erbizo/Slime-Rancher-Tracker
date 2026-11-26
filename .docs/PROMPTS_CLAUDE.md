# Slime Rancher Tracker — Prompts para Claude Code

Guía de prompts para la **Entrega 1 con IA (Acceso a Datos)**, pensada para cubrir el tramo alto de cada objetivo de la rúbrica.

> **Ojo:** el enunciado nombra a **Junie** como herramienta de apoyo y el objetivo 4 habla de "Auditoría de Código con Junie". Antes de empezar, confirma con tu profesor que puedes usar Claude Code. Si te dice que sí, explícalo al principio de `AI_WORKFLOW.md` (qué herramienta usaste y por qué).

## Decisiones tomadas

| Tema | Decisión |
|---|---|
| Qué registra | Por partida: **zonas desbloqueadas**, **slimes descubiertos** y **logros** (marcados a mano). Nada de dinero, corrales, gordos ni mercado |
| Juego | Slime Rancher 1 |
| Usuarios | Login con registro abierto, sesión con `HttpSession`, contraseñas con BCrypt |
| Partidas | Cada usuario puede tener varias (1:N) |
| Catálogo | Zonas, slimes y logros de solo lectura, cargados desde `init.sql` |
| Backend | Java 21, Maven (WAR), Tomcat 10.1, Jersey 3 (JAX-RS), JDBC puro + HikariCP |
| Base de datos | MariaDB 11.4 (+ Adminer) |
| Tests | JUnit 5 + Mockito + H2 en modo MariaDB + JaCoCo |
| Frontend | Varias páginas, HTML + CSS + JS vanilla, **modo claro y oscuro** (sigue al sistema + botón para cambiarlo). Diseño **ya aprobado** en `.docs/diseno/` (guía, capturas, HTML de referencia y datos de prueba), hecho con la skill **charles-unslop**. Hay que reproducirlo tal cual |
| Código | En español |
| Skills | **karpathy-guidelines** en todas las fases de código; **charles-unslop** en la fase de interfaz |

## Cómo usar este documento

1. **No pegues todo de golpe.** La rúbrica premia commits atómicos y un `AI_WORKFLOW.md` con varios prompts clave y correcciones manuales. Trabaja por fases: prompt → revisas → corriges → commit → anotas en `AI_WORKFLOW.md`.
2. El **Prompt 0** va en `CLAUDE.md`, en la raíz del proyecto. Claude Code lo carga en cada conversación. Copia también la carpeta **`.docs/`** a la raíz del proyecto antes de empezar: ahí está el diseño aprobado.
3. En cada fase activa el **modo plan** (Shift+Tab, o el selector de modo en la app) antes de pegar el prompt. Gracias a karpathy-guidelines, Claude te dirá qué está suponiendo y te enseñará el plan antes de tocar nada. Lo que corrijas de ese plan es una decisión de arquitectura para `AI_WORKFLOW.md`.
4. Conversación nueva (`/clear`) en cada fase: contexto limpio y `CLAUDE.md` mantiene las reglas.
5. Después de cada fase hay una lista **"Revisa tú"** con los fallos típicos de un agente. Lo que encuentres y corrijas es lo que `AI_WORKFLOW.md` tiene que contar ("errores corregidos manualmente"). Apunta los reales.
6. Haz tú los commits con los mensajes sugeridos. Claude no hace commits.

---

## PROMPT 0 — `CLAUDE.md`

```markdown
# Proyecto: Slime Rancher Tracker

Web para que jugadores de Slime Rancher 1 lleven el control de su progreso. Cada usuario se
registra, tiene varias partidas y en cada una marca: zonas desbloqueadas, slimes descubiertos
(Slimepedia) y logros conseguidos. La web muestra un panel con su porcentaje de progreso.
No se guarda dinero, corrales, gordos ni ventas: solo esas tres cosas.
Es una práctica de "Acceso a Datos" (2º DAM). Yo soy el arquitecto y revisor; tú implementas.

## Skills
- Usa SIEMPRE la skill `karpathy-guidelines` al escribir, revisar o refactorizar código:
  di qué estás suponiendo antes de programar, haz lo mínimo que cumpla el requisito,
  toca solo lo necesario y define criterios de éxito verificables.
- Usa la skill `charles-unslop` en todo lo relacionado con la interfaz web (fase 5). La dirección
  visual ya está decidida y aprobada en `.docs/diseno/`: la skill sirve para respetarla y auditar,
  no para inventar otra.

## Diseño aprobado (obligatorio)
- La interfaz tiene que quedar como `.docs/diseno/`: lee `GUIA_VISUAL.md`, mira `capturas/*.png`
  y usa `pantallas/estilos.css` y `pantallas/tema.js` como base. Mismos colores (modo claro y
  oscuro), tipografías (Nunito + Fredoka), formas (slime y plort), estructura de cada pantalla y textos.
- Todos los colores de la interfaz salen de los tokens CSS de `:root`; ninguno escrito a mano.
- Los datos de prueba de `init.sql` son los de `.docs/diseno/DATOS_DEMO.md`.
- No cambies colores, tipografías, layout ni textos de la interfaz sin preguntarme antes.

## Stack (no cambiar sin preguntar)
- Java 21, Maven, empaquetado WAR, desplegado en Tomcat 10.1.
- Jakarta EE 10: Jersey 3.1 (JAX-RS) + Jackson. Bean Validation NO: valida a mano en servicio.
- JDBC puro con PreparedStatement + pool HikariCP. Driver mariadb-java-client. NADA de JPA.
- MariaDB 11.4. Contraseñas con BCrypt (org.mindrot:jbcrypt).
- Frontend: HTML5 semántico + CSS3 + JavaScript vanilla (ES modules) con fetch. Sin frameworks ni CDNs.
- Tests: JUnit 5, Mockito, AssertJ, H2 (MODE=MariaDB). JaCoCo para cobertura.
- Docker multi-stage + Docker Compose (db, app, adminer).

## Paquetes (base: com.slimetracker)
- config    → ConfigBD (lee variables de entorno), ProveedorDataSource (Hikari), ContextoApp
              (crea a mano DataSource → GestorTransacciones → DAOs → Servicios), listener de arranque/parada.
- modelo    → Entidades = tablas. Nunca salen de la capa servicio.
- dto       → Records de entrada (XxxPeticion) y salida (XxxRespuesta).
- mapeador  → Entidad <-> DTO.
- dao       → Interfaces DAO; dao/jdbc → implementaciones JDBC.
- servicio  → Lógica de negocio, validaciones y transacciones.
- recurso   → Endpoints JAX-RS: reciben, delegan en el servicio y devuelven Response. Nada más.
- seguridad → Filtro de autenticación, usuario en sesión, BCrypt.
- excepcion → Excepciones propias + ExceptionMappers.
- tx        → GestorTransacciones.
Dependencias: recurso → servicio → dao → BD. Sin saltos de capa ni ciclos. Los recursos solo ven DTOs.

## Reglas obligatorias
1. Cero credenciales/URLs hardcodeadas: System.getenv() de DB_HOST, DB_PORT, DB_NAME, DB_USER,
   DB_PASSWORD. Si falta una, fallar al arrancar con un mensaje claro.
2. Siempre PreparedStatement con `?`. Prohibido concatenar datos en SQL.
3. Siempre try-with-resources para Connection, PreparedStatement y ResultSet.
4. Transacciones con GestorTransacciones: `<T> T enTransaccion(FuncionSql<Connection,T> trabajo)`
   (setAutoCommit(false), commit, rollback ante cualquier excepción, restaurar autocommit).
   Los métodos DAO reciben la Connection como primer parámetro.
5. Las SQLException se traducen en el DAO a excepciones propias unchecked: AccesoDatosException,
   EntidadDuplicadaException (1062), IntegridadReferencialException (1451/1452).
   En servicio: NoEncontradoException, ValidacionException (mapa campo→mensaje),
   ReglaNegocioException, NoAutenticadoException, AccesoDenegadoException.
6. Códigos HTTP: 200 GET/PUT, 201 POST + cabecera Location, 204 DELETE, 400 validación,
   401 sin sesión o login incorrecto, 403 partida de otro usuario, 404 no existe,
   409 duplicado o regla de negocio incumplida, 500 inesperado.
   Error JSON uniforme: {"status":400,"error":"Bad Request","mensaje":"...","campos":{...},"timestamp":"..."}
   Los 500 se registran con stack trace en el log pero NO se devuelve al cliente.
7. Validación siempre en servidor. La del frontend es solo de comodidad.
8. Nunca devolver el hash de la contraseña en ningún DTO ni log.
9. Nada de Java ni SQL en las vistas. Nada de lógica de negocio en los recursos.
10. Código, tablas, comentarios y textos de la interfaz en español.
11. No uses imágenes, logos, fuentes ni sprites oficiales del juego.
12. Antes de dar una fase por terminada, compila y prueba tú mismo (mvn, docker compose, curl)
    y arregla lo que falle. Después: lista de archivos tocados, decisiones no obvias, qué has
    probado y cómo lo compruebo yo. Luego PARA; no empieces la siguiente fase sin que te lo pida.
13. NO hagas git commit ni push. Propón 2-3 mensajes de commit (Conventional Commits)
    diciendo qué archivos van en cada uno.
```

---

## FASE 1 — Esqueleto, Docker y base de datos

```text
Fase 1: esqueleto del proyecto e infraestructura. Sigue CLAUDE.md y la skill karpathy-guidelines.

1. pom.xml (war, Java 21, finalName slime-tracker): jakarta.servlet-api (provided),
   jersey-container-servlet, jersey-hk2, jersey-media-json-jackson, jackson-datatype-jsr310,
   HikariCP, mariadb-java-client, jbcrypt, slf4j-simple. Test: junit-jupiter,
   mockito-junit-jupiter, assertj-core, h2. Plugins: war, surefire, jacoco (informe en `mvn verify`).

2. db/init.sql para MariaDB, InnoDB, utf8mb4, con CHECKs. Tablas:
   - usuario(id PK AI, nombre_usuario VARCHAR(30) UNIQUE NOT NULL, hash_contrasena CHAR(60) NOT NULL,
     creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - partida(id PK AI, usuario_id FK→usuario ON DELETE CASCADE ON UPDATE CASCADE,
     nombre VARCHAR(40) NOT NULL, creada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     actualizada_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     UNIQUE(usuario_id, nombre))                                                   [1:N]
   - zona(id PK AI, nombre VARCHAR(50) UNIQUE NOT NULL, descripcion VARCHAR(255),
     orden INT UNIQUE NOT NULL, es_inicial BOOLEAN NOT NULL DEFAULT FALSE,
     zona_requisito_id FK→zona NULL ON DELETE RESTRICT)                             [1:N reflexiva]
   - slime(id PK AI, nombre VARCHAR(50) UNIQUE NOT NULL,
     dieta ENUM('CARNE','FRUTA','VERDURA','OMNIVORO','ESPECIAL') NOT NULL,
     comida_favorita VARCHAR(60), rareza ENUM('COMUN','RARO','ESPECIAL') NOT NULL,
     peligroso BOOLEAN NOT NULL DEFAULT FALSE, descripcion VARCHAR(255),
     color CHAR(7) NOT NULL (hex de la tabla de colores de .docs/diseno/GUIA_VISUAL.md),
     zona_id FK→zona ON DELETE RESTRICT ON UPDATE CASCADE)                          [1:N]
   - logro(id PK AI, nombre VARCHAR(60) UNIQUE NOT NULL, descripcion VARCHAR(255) NOT NULL,
     categoria ENUM('EXPLORACION','SLIMES','RANCHO','SECRETOS') NOT NULL)
   - partida_zona(partida_id FK CASCADE, zona_id FK RESTRICT, desbloqueada_en TIMESTAMP,
     PK(partida_id, zona_id))                                                      [N:M]
   - partida_slime(partida_id FK CASCADE, slime_id FK RESTRICT, descubierto_en TIMESTAMP,
     nota VARCHAR(200) NULL, PK(partida_id, slime_id))                             [N:M]
   - partida_logro(partida_id FK CASCADE, logro_id FK RESTRICT, conseguido_en TIMESTAMP,
     nota VARCHAR(200) NULL, PK(partida_id, logro_id))                             [N:M]
   Índices en todas las FK.
   CHECKs mínimos: color REGEXP '^#[0-9A-F]{6}$', CHAR_LENGTH(nombre) > 0 en partida, zona y slime,
   zona_requisito_id <> id.
   Datos (DML): EXACTAMENTE los de .docs/diseno/DATOS_DEMO.md (7 zonas con requisito, 18 slimes
   con zona/dieta/rareza/color, 15 logros, usuarios `rancher` y `vecina` con sus partidas y
   progreso, fechas relativas a NOW()). Los hash BCrypt tienen que ser reales: genéralos, no los
   inventes. Si algún requisito de zona o zona de un slime no te cuadra con el juego, márcalo con
   un comentario en el SQL para que lo revise yo; no lo cambies tú.

3. Dockerfile multi-stage: build con maven:3.9-eclipse-temurin-21 (copiar primero pom.xml y
   `mvn dependency:go-offline` para cachear, luego src y `mvn -q package -DskipTests`);
   runtime con tomcat:10.1-jre21-temurin, borrar webapps por defecto, copiar el WAR como ROOT.war,
   usuario no root, EXPOSE 8080. Añade .dockerignore (target, .git, .idea, .env).

4. docker-compose.yml:
   - db: mariadb:11.4, MARIADB_* desde .env, volumen nombrado db_data:/var/lib/mysql,
     ./db/init.sql en /docker-entrypoint-initdb.d/01-init.sql:ro,
     healthcheck ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
     (interval 10s, timeout 5s, retries 10, start_period 30s), solo red backend, sin puertos.
   - app: build ., depends_on db con condition: service_healthy, DB_* desde .env (DB_HOST=db),
     puerto ${APP_PORT}:8080, redes backend y frontend, restart unless-stopped.
   - adminer: adminer, depends_on db healthy, puerto ${ADMINER_PORT}:8080, redes backend y frontend.
   - networks: backend con internal: true; frontend normal. volumes: db_data.

5. .env.example con todas las variables comentadas (MARIADB_ROOT_PASSWORD, MARIADB_DATABASE,
   MARIADB_USER, MARIADB_PASSWORD, DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, APP_PORT=8080,
   ADMINER_PORT=8081). .gitignore para Maven, IntelliJ, VS Code y .env.

6. config/ConfigBD, ProveedorDataSource (Hikari, cerrado en un ServletContextListener), la clase
   Application de Jersey en /api y GET /api/salud que hace SELECT 1 y devuelve {"estado":"OK"}.

Criterios de éxito:
- `docker compose up --build -d` → los 3 servicios arriba y db en estado healthy antes que app.
- `curl http://localhost:8080/api/salud` → 200 {"estado":"OK"}.
- Adminer en http://localhost:8081 muestra las 8 tablas con datos.
- `grep` de contraseñas en el código Java y en docker-compose.yml → ninguna.
```

**Revisa tú:**
- Los requisitos de zona que Claude marque como dudosos: corrígelos tú con lo que sabes del juego (es un error corregido a mano real para el `AI_WORKFLOW.md`).
- Que el hash del usuario demo funcione de verdad (lo comprobarás en la fase 4 al hacer login).
- Contenedor con usuario no root: Tomcat tiene que poder escribir en `logs/`, `temp/`, `work/` y `webapps/`. Si la app no arranca, mira `docker compose logs app`: suele ser un `chown` que falta en el Dockerfile.
- `.env` fuera de git, `.env.example` dentro.
- Si cambias el `init.sql`, hay que borrar el volumen: `docker compose down -v`.

**Commits:** `chore: estructura Maven inicial` · `feat(db): esquema y datos de Slime Rancher 1` · `build: Dockerfile multi-stage y docker-compose`

---

## FASE 2 — Modelo, DTOs y DAO (JDBC)

```text
Fase 2: capa de acceso a datos. Sigue CLAUDE.md y karpathy-guidelines.

1. Entidades en modelo/ para las 8 tablas.
2. Excepciones de datos en excepcion/ y TraductorSqlException (errorCode 1062 → duplicado,
   1451/1452 → integridad referencial, resto → AccesoDatosException).
3. tx/GestorTransacciones con enTransaccion(...) y soloLectura(...). Interfaz FuncionSql que
   permite lanzar SQLException.
4. DAOs (todos los métodos reciben Connection):
   - UsuarioDao: insertar, buscarPorId, buscarPorNombre.
   - PartidaDao: CRUD + listarPorUsuario + tocar(id) (actualiza actualizada_en).
   - ZonaDao: listar, buscarPorId, listarDesbloqueadas(partidaId), desbloquear, bloquear,
     estaDesbloqueada, listarDependientesDesbloqueadas(partidaId, zonaId).
   - SlimeDao: listar con filtros opcionales (zona, dieta, rareza), buscarPorId,
     listarConEstado(partidaId) (LEFT JOIN para saber si está descubierto),
     descubrir, olvidar, actualizarNota, estaDescubierto, contarDescubiertosEnZona(partidaId, zonaId).
   - LogroDao: listar, listarConEstado(partidaId), marcar, desmarcar, actualizarNota.
   - ProgresoDao (solo lectura): totales y conseguidos de zonas, slimes y logros; desglose de
     slimes por zona; últimas 10 actividades de la partida (UNION de las tres tablas puente
     ordenado por fecha).
   findById devuelve Optional. Mapeo del ResultSet en métodos privados.
5. DTOs (records) y mapeadores: UsuarioRespuesta (sin hash), PartidaPeticion/Respuesta,
   ZonaEstadoRespuesta (con desbloqueada y requisito), SlimeEstadoRespuesta (con color),
   LogroEstadoRespuesta,
   NotaPeticion, DescubrirVariosPeticion(List<Long> slimeIds), ProgresoRespuesta.

Criterios de éxito: `mvn compile` sin errores; ningún SQL concatena datos; ninguna entidad se
importa fuera de dao, servicio y mapeador.
No hagas aún servicios ni endpoints.
```

**Revisa tú:**
- Busca `" +` dentro de las cadenas SQL: no debe haber datos concatenados.
- Cada `ResultSet` en su propio try-with-resources.
- Que `listarConEstado` sea una sola consulta con `LEFT JOIN` y no una consulta por slime (problema N+1).

**Commits:** `feat(modelo): entidades y DTOs` · `feat(dao): DAOs JDBC y traducción de errores SQL` · `feat(tx): gestor de transacciones`

---

## FASE 3 — Servicios y reglas de negocio

```text
Fase 3: servicios. Reciben DAOs y GestorTransacciones por constructor (para Mockito). Hablan en
DTOs hacia fuera. ValidacionException devuelve TODOS los errores de campo a la vez.
Todos los métodos de partida reciben el id del usuario en sesión y comprueban que la partida es
suya (si no existe → 404; si es de otro → 403).

Reglas:
R1. Registro: nombre_usuario 3–30 caracteres [a-zA-Z0-9_], contraseña mínimo 8 con al menos una
    letra y un número. Duplicado → 409. En UNA transacción: crea el usuario (BCrypt) y una primera
    partida "Mi rancho" con la zona inicial desbloqueada.
R2. Login: usuario o contraseña incorrectos → 401 con el mismo mensaje genérico en los dos casos.
R3. Partida: nombre 1–40, único por usuario (409). Crear partida = insertar + desbloquear la zona
    inicial, en una transacción.
R4. Desbloquear zona: la zona existe (404), no está ya desbloqueada (409) y su zona requisito sí
    lo está (409: "Primero tienes que desbloquear X").
R5. Bloquear zona: no se puede bloquear la inicial (409), ni una zona de la que dependan otras
    zonas desbloqueadas, ni una con slimes descubiertos (409 diciendo cuáles).
R6. Descubrir slime: su zona tiene que estar desbloqueada (409) y no estar ya descubierto (409).
R7. Descubrir varios slimes a la vez: todo o nada en una transacción; si uno falla, no se guarda
    ninguno y el error dice cuál y por qué.
R8. Logros: marcar (409 si ya está), desmarcar, editar nota (máx. 200 caracteres).
R9. Cualquier cambio de progreso actualiza partida.actualizada_en en la misma transacción.
R10. Progreso: % zonas, % slimes, % logros, % global (media de los tres, redondeada a entero con
     HALF_UP: la Primera partida del demo da 58), slimes descubiertos por
     zona, últimas 10 actividades y "siguientes pasos": zonas que ya puedes desbloquear y slimes
     que ya puedes descubrir porque su zona está abierta.

Servicios: AutenticacionServicio, PartidaServicio, ZonaServicio, SlimeServicio, LogroServicio,
ProgresoServicio.

Criterios de éxito: `mvn compile` sin errores; cada regla R1–R10 tiene un método identificable;
ningún servicio depende de otro en círculo.
```

**Revisa tú:**
- Que la comprobación de propiedad (403) esté en **todos** los métodos de partida, no solo en algunos. Es el fallo de seguridad más típico.
- Que R7 sea realmente todo o nada (una sola transacción, no un bucle de transacciones).
- Que ningún mensaje de error enseñe SQL ni nombres de tablas.

**Commits:** `feat(servicio): registro, login y partidas` · `feat(servicio): reglas de zonas, slimes y logros` · `feat(servicio): cálculo de progreso`

---

## FASE 4 — Autenticación y API REST

```text
Fase 4: seguridad y recursos JAX-RS. Sigue CLAUDE.md y karpathy-guidelines.

Seguridad:
- Filtro (ContainerRequestFilter) que exige sesión en todo /api salvo /api/auth/registro,
  /api/auth/login y /api/salud → 401 si no hay sesión.
- Login: request.changeSessionId() para evitar fijación de sesión y guardar solo el id y el
  nombre del usuario en la sesión. Logout invalida la sesión.
- META-INF/context.xml con <CookieProcessor sameSiteCookies="strict"/>. Sesión de 30 min en web.xml.

Endpoints (JSON, bajo /api):
- POST /auth/registro (201), POST /auth/login (200), POST /auth/logout (204), GET /auth/yo
- GET /zonas, GET /slimes (?zonaId&dieta&rareza), GET /logros       (catálogo, solo lectura)
- GET/POST /partidas, GET/PUT/DELETE /partidas/{id}
- GET /partidas/{id}/progreso
- GET /partidas/{id}/zonas, POST /partidas/{id}/zonas/{zonaId}, DELETE /partidas/{id}/zonas/{zonaId}
- GET /partidas/{id}/slimes (?zonaId&descubierto=true|false), POST /partidas/{id}/slimes/{slimeId},
  POST /partidas/{id}/slimes (varios, body {"slimeIds":[...]}), PUT /partidas/{id}/slimes/{slimeId}/nota,
  DELETE /partidas/{id}/slimes/{slimeId}
- GET /partidas/{id}/logros, POST /partidas/{id}/logros/{logroId}, PUT .../{logroId}/nota,
  DELETE .../{logroId}

Validación de entrada en la capa de control (lo que pide el enunciado para esta capa): ids de
ruta numéricos y positivos, cuerpo presente cuando el endpoint lo necesita, JSON bien formado y
tipos correctos → 400 antes de llamar al servicio. Las reglas de negocio siguen en el servicio.

ExceptionMappers para todas las excepciones propias, JSON mal formado (400), ruta inexistente (404)
y Throwable (500 genérico). Formato de error uniforme. Jackson con fechas ISO-8601.

Crea docs/api.http con una petición por endpoint, incluyendo login y casos de error
(400, 401, 403, 404, 409).

Criterios de éxito: todas las peticiones de docs/api.http devuelven el código esperado contra
`docker compose up`; sin sesión cualquier /api/partidas da 401; con `rancher`, leer o modificar
la partida de `vecina` da 403; GET /api/partidas/{id}/progreso de la Primera partida devuelve 58.
```

**Revisa tú:**
- Entra como `vecina` y prueba a leer/modificar las partidas de `rancher`: tiene que dar 403 en todo.
- Envía JSON roto y un id que no existe: 400 y 404, no un 500.
- En las DevTools, la cookie `JSESSIONID` tiene que salir como `HttpOnly` y `SameSite=Strict`.

**Commits:** `feat(seguridad): sesión, filtro de autenticación y BCrypt` · `feat(api): endpoints REST` · `feat(api): manejo centralizado de errores`

---

## FASE 5 — Interfaz web (con charles-unslop)

El diseño ya está aprobado en `.docs/diseno/` (lo hicimos con un wireframe de alta fidelidad y charles-unslop). Esta fase va en **tres prompts**: entender el diseño, construirlo y comprobar que ha quedado igual.

### 5a — Leer el diseño aprobado

```text
Fase 5a: NO escribas código todavía. Lee .docs/diseno/GUIA_VISUAL.md, abre todas las imágenes de
.docs/diseno/capturas/ y lee .docs/diseno/pantallas/ (HTML + estilos.css). Ese diseño está
aprobado y tiene que quedar igual.

Devuélveme:
1. Un resumen de la dirección en tus palabras (colores, tipografías, slime, plort, momento).
2. El mapa de archivos que vas a crear en src/main/webapp (páginas, css, módulos js) y qué parte de
   pantallas/*.html reutilizas en cada uno.
3. Qué partes de los HTML de referencia son estados fijos de ejemplo y cómo los harás dinámicos
   con la API (por ejemplo: el aviso 409 de zonas, los errores de registro, la confirmación de borrado).
4. Cualquier cosa del diseño que choque con la API o las reglas R1–R10, para decidirla yo.
Espera mi aprobación.
```

### 5b — Construcción

```text
Fase 5b: construye el frontend en src/main/webapp reproduciendo EXACTAMENTE .docs/diseno/
(GUIA_VISUAL.md, capturas y pantallas/). Sigue karpathy-guidelines y las reglas de charles-unslop
de la guía (nada de gradientes, emojis, colores nuevos ni pesos 700+).

Estructura:
- css/: parte de .docs/diseno/pantallas/estilos.css (mismos tokens y clases, modo claro y
  oscuro). Solo añade lo que necesiten los estados dinámicos (cargando, vacío) con los mismos tokens.
- js/tema.js: el de .docs/diseno/pantallas/tema.js, cargado en <head> sin defer en todas las
  páginas (sigue al sistema; el botón sol/luna fuerza un modo y lo guarda en localStorage).
- El color de cada slime viene de la API (campo color) y se pinta con la variable CSS --color.
- js/api.js: wrapper de fetch con credentials 'same-origin', parseo del error uniforme a
  ApiError {status, mensaje, campos}, timeout de 10 s con AbortController. Un 401 redirige a login.
- js/ui.js: avisos con aria-live, estados de carga, estados vacíos, confirmación accesible antes
  de borrar, errores de campo bajo cada input. Escapa SIEMPRE el texto del usuario (nada de
  innerHTML con datos: usa textContent o crea nodos).
- js/estado.js: partida seleccionada en localStorage (envuelto en try/catch).
- Páginas (cada una como su referencia en .docs/diseno/pantallas/):
  login.html (entrar.html), registro.html, partidas.html, index.html (panel.html), zonas.html,
  slimepedia.html, logros.html. Misma cabecera común; el selector "Partida" lleva a partidas.html
  y en partidas.html se muestra "Conectado como <usuario>" como texto.
- Comportamiento: el que describe GUIA_VISUAL.md para cada pantalla (estados del detalle de la
  Slimepedia, aviso de 409 en zonas, check con forma de plort en logros, confirmación en línea
  antes de borrar una partida, todos los errores de registro a la vez).
UX obligatoria: cada botón se desactiva mientras su petición está en curso; errores 400 junto a
su campo y el resto como aviso con el mensaje del servidor; tras cada cambio se refresca lo
afectado; responsive desde 375 px; accesible por teclado con foco visible; CERO errores en consola.

Criterios de éxito: todas las páginas cargan sin errores en consola ni 404 de recursos; el flujo
registro → crear partida → desbloquear zona → descubrir slime → marcar logro → ver panel funciona
entero; a 375 px no hay scroll horizontal; con los datos del usuario demo, cada página se ve como
su captura de .docs/diseno/capturas/ en modo claro y en modo oscuro; el modo elegido se mantiene
al cambiar de página y al recargar.
```

### 5c — Comparación con el diseño y auditoría

```text
Fase 5c: comprueba que la web real es igual que el diseño aprobado. Con los datos del usuario demo,
haz capturas de cada página a 1280 px, del panel a 390 px y de panel, Slimepedia, zonas y entrar
en modo oscuro (si no tienes navegador, dime cuáles te paso yo) y ponlas al lado de su captura de
.docs/diseno/capturas/ al mismo tamaño.
Para cada página: lista las diferencias (color, tipografía, espaciado, tamaños, textos, orden) y
corrígelas hasta que coincidan. Después pasa la auditoría final de charles-unslop solo para
detectar señales que se hayan colado (gradientes, emojis, pesos altos, divisores sobrantes…),
sin cambiar la dirección aprobada. Dame el informe: diferencias encontradas y qué corregiste.
```

**Revisa tú:**
- Abre DevTools → Consola y Red en cada página: ni un error rojo.
- Escribe `<b>hola</b>` como nombre de partida o nota: tiene que verse como texto, no en negrita (si no, hay XSS).
- Para la base de datos (`docker compose stop db`) y usa la web: tiene que salir un mensaje claro, no quedarse colgada.
- Abre la web y la captura de `.docs/diseno/capturas/` una al lado de la otra: lo que no coincida y corrijas es un "error corregido a mano" para el `AI_WORKFLOW.md`.
- Cambia a modo oscuro y recorre todas las páginas: ningún texto ilegible ni fondo blanco suelto (suele pasar con colores escritos a mano en vez de tokens).
- Guarda las capturas de la web final para el README.

**Commits:** `docs(diseno): wireframe aprobado en .docs/diseno` (hazlo antes de empezar la fase) · `feat(web): cliente API y componentes base` · `feat(web): login, registro y partidas` · `feat(web): panel, zonas, Slimepedia y logros` · `style(web): ajustes tras comparar con el diseño`

---

## FASE 6 — Batería de pruebas

```text
Fase 6: tests con JUnit 5. Sigue karpathy-guidelines (cada regla, un test que la demuestre).
`mvn verify` debe pasar sin Docker y generar el informe JaCoCo.

1. Integración DAO con H2 (jdbc:h2:mem:test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1).
   src/test/resources/schema-h2.sql equivalente a init.sql y una clase base que recrea esquema y
   datos antes de cada test. Por cada DAO, las operaciones CRUD que tenga (crear, leer, actualizar,
   borrar, listar) en camino feliz, más estos casos límite:
   - nombre de usuario duplicado y partida con nombre repetido para el mismo usuario
     → EntidadDuplicadaException; mismo nombre de partida en otro usuario → permitido
   - buscarPorId inexistente → Optional.empty(); update/delete inexistente → 0 filas
   - descubrir dos veces el mismo slime → EntidadDuplicadaException
   - borrar partida → cascada en partida_zona, partida_slime y partida_logro
   - GestorTransacciones: una excepción a mitad deshace TODO
2. Unitarios de servicios con Mockito (el GestorTransacciones mockeado ejecuta la lambda):
   cada regla R1–R10 en camino feliz y en cada error (validación con varios campos mal, 401,
   403, 404, 409 de cada tipo, R7 que no guarda nada si un slime falla, mensaje genérico de login).
   Comprueba con verify(..., never()) que no se escribe nada cuando una validación falla.
3. Tests de los ExceptionMapper y del filtro de autenticación.
4. @DisplayName en español, @Nested por funcionalidad, @ParameterizedTest para validaciones.

Criterio de éxito: `mvn clean verify` en verde y ≥ 80 % de líneas en servicio y dao.
Dime el porcentaje real.
```

**Revisa tú:**
- Abre `target/site/jacoco/index.html` y guarda una captura para el README.
- Rompe a propósito una regla (p. ej. quita la comprobación de 403) y comprueba que algún test falla. Si ninguno falla, el test no sirve.
- Si `schema-h2.sql` se aleja mucho de `init.sql` (H2 no entiende algo de MariaDB), que Claude te diga qué ha cambiado y por qué: es una decisión que va al `AI_WORKFLOW.md`.

**Commits:** `test(dao): integración con H2 y casos límite` · `test(servicio): reglas de negocio con Mockito` · `test(api): mappers y filtro de seguridad`

---

## FASE 7 — Auditoría de código

Cubre la "Auditoría de Código" del objetivo 4 (que el enunciado plantea con Junie). Hazla en una conversación nueva para que Claude revise sin el sesgo de haberlo escrito.

```text
Fase 7: actúa como revisor exigente usando karpathy-guidelines. NO cambies nada todavía.
Audita el proyecto entero y devuélveme una tabla (archivo:línea, problema, gravedad, propuesta):
- SQL concatenado, recursos sin try-with-resources, transacciones sin rollback.
- Entidades fuera del servicio, lógica en recursos, dependencias entre capas incorrectas.
- Credenciales o URLs hardcodeadas en archivos versionados.
- Seguridad: métodos sin comprobación de propiedad, hash en respuestas o logs, XSS en el JS,
  sesión sin regenerar, cookies sin HttpOnly/SameSite.
- Códigos HTTP incorrectos o 500 donde toca 400/401/403/404/409.
- Exceso de complejidad: abstracciones de un solo uso, código especulativo, duplicado,
  métodos de más de 40 líneas, imports sin usar.
- Docker: capas mal cacheadas, contenedor como root, healthcheck o depends_on mal puestos.
Luego espera a que yo elija qué arreglar.
```

Elige qué arreglar y qué no (con motivo), pídeselo y documéntalo en `AI_WORKFLOW.md`.

**Commits:** uno por arreglo, p. ej. `fix(seguridad): comprobar propiedad al editar notas de logros`

---

## FASE 8 — Documentación

```text
Fase 8: README.md profesional en español con:
1. Qué es y capturas (marcadores docs/img/*.png que rellenaré yo).
2. Stack y arquitectura por capas en Mermaid (navegador → recurso → servicio → dao → MariaDB).
3. Diagrama entidad-relación en Mermaid (erDiagram) de las 8 tablas, con cardinalidades y
   explicación de cada relación y de cada ON DELETE.
4. Esquema de contenedores en Mermaid: app, db, adminer, redes backend (internal) y frontend,
   volumen db_data y puertos.
5. Despliegue paso a paso: copiar .env.example a .env (bash y PowerShell), tabla de variables,
   docker compose up --build -d, docker compose ps, docker compose logs -f app.
6. URLs, puertos y credenciales de prueba: app en http://localhost:8080 (usuarios `rancher` /
   `slime1234` y `vecina` / `slime5678`), Adminer en http://localhost:8081 (servidor `db` y
   credenciales del .env.example).
7. Persistencia demostrada: crear una partida, docker compose down, docker compose up -d, sigue
   ahí; docker compose down -v para volver a los datos de init.sql.
8. Manual breve de cada página.
9. Tabla de la API (endpoint, método, códigos) y enlace a docs/api.http.
10. Reglas de negocio R1–R10.
11. Tests: mvn verify y dónde ver la cobertura.
12. Estructura de carpetas.

Crea también AI_WORKFLOW.md con esta plantilla. Rellena solo lo que sabes y deja TODO en lo que
tengo que escribir yo:
- Herramienta (Claude Code en lugar de Junie) y metodología: fases, CLAUDE.md, modo plan, skills
  karpathy-guidelines y charles-unslop. TODO: motivo y autorización del profesor.
- Por fase: prompt (resumen + enlace a PROMPTS_CLAUDE.md), plan propuesto y qué cambié,
  qué generó Claude, TODO: errores que encontré y cómo los corregí, TODO: decisiones de arquitectura.
- Diseño: wireframe hecho antes de programar (enlace a .docs/diseno/), dirección de
  charles-unslop y diferencias que hubo que corregir al comparar la web con las capturas.
- Auditoría de la fase 7 y qué se aplicó.
- TODO: conclusiones.
```

**Revisa tú:**
- Clona el repo en otra carpeta y sigue el README desde cero. Si un comando falla, pierdes nota en el objetivo 6.
- Los diagramas Mermaid se ven bien en GitHub.
- Rellena los TODO de `AI_WORKFLOW.md` con lo que de verdad corregiste.

**Commits:** `docs: README con ER, arquitectura y despliegue` · `docs: registro AI_WORKFLOW`

---

## Checklist final contra la rúbrica

| Objetivo | Pts | Qué lo asegura |
|---|---|---|
| 1. BD y DAO | 2,5 | 8 tablas, 1:N (usuario-partida, zona-slime, zona reflexiva) y 3 N:M, ON DELETE/UPDATE, CHECKs, `init.sql` automático, PreparedStatement, HikariCP, try-with-resources, transacciones, excepciones propias |
| 2. Capas | 2,5 | recurso → servicio → dao, DTOs, `.env`, validación en servidor, 200/201/204/400/401/403/404/409/500 |
| 3. Interfaz | 1,5 | Diseño aprobado en `.docs/diseno/` reproducido y comparado con capturas (claro y oscuro), responsive, estados de carga, errores por campo, cero errores de consola |
| 4. Tests + IA | 1 | DAO con H2 + servicios con Mockito, casos límite, JaCoCo, auditoría (fase 7) y `AI_WORKFLOW.md` |
| 5. Docker | 1,5 | Multi-stage, healthcheck + `service_healthy`, red `internal`, volumen nombrado, persistencia demostrada |
| 6. Docs | 1 | README con ER, contenedores, `.env` paso a paso, puertos, credenciales y manual |

**Antes de entregar:**
- [ ] `docker compose down -v` y `docker compose up --build -d` desde un clon limpio funcionan a la primera.
- [ ] `mvn clean verify` en verde.
- [ ] `git log --oneline` muestra commits pequeños y descriptivos.
- [ ] `.env` fuera del repo; `.env.example` dentro.
- [ ] Zip sin `target/` ni `.git/`:

```powershell
git archive --format=zip --output=slime-tracker.zip HEAD
```
