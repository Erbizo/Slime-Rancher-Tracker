# Proyecto: Slime Rancher Tracker

Web para que jugadores de Slime Rancher 1 lleven el control de su progreso. Cada usuario se
registra, tiene varias partidas y en cada una marca: zonas desbloqueadas, slimes descubiertos
(Slimepedia) y logros conseguidos. La web muestra un panel con su porcentaje de progreso.
No se guarda dinero, corrales, gordos ni ventas: solo esas tres cosas.
Es una práctica de "Acceso a Datos" (2º DAM). Yo soy el arquitecto y revisor; tú implementas.

## Documentación del proyecto
- Requisitos y rúbrica de la práctica: `.docs/Entrega 1 con IA - Acceso a datos.pdf`. Léelo antes de
  cada fase y avísame si algo de lo que te pido choca con él.
- Prompts de cada fase: `.docs/PROMPTS_CLAUDE.md`.
- Diseño aprobado y datos de prueba: `.docs/diseno/`.
- El proyecto ya tiene `pom.xml`, `src/` y `.gitignore` creados por IntelliJ: adáptalos en vez de
  crearlos de nuevo.

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
