# nexus_inventory_api
Proyecto academico para la asignatura de Software III.

## Variables de entorno
La aplicacion carga variables desde `.env` usando:

`spring.config.import=optional:file:.env[.properties]`

El esquema fue estandarizado asi:

- `GLOBAL`: variables compartidas sin prefijo.
- `DEV_`: desarrollo local.
- `TEST_`: pruebas.
- `PROD_`: produccion / Render.

Para desarrollo local, el `.env` puede dejar configurado:

- `SPRING_PROFILES_DEFAULT=dev`

Variables minimas requeridas:

- `DEV_DB_URL`
- `DEV_DB_USERNAME`
- `DEV_DB_PASSWORD`
- `DEV_JWT_SECRET`
- `DEV_JWT_EXPIRATION`
- `TEST_JWT_SECRET`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_PROTOCOL`

Compatibilidad temporal:

- Los `application*.properties` aceptan los nombres legacy anteriores como fallback.
- El `.env` y `.env.example` documentan el mapeo recomendado para completar la migracion.

## Primer usuario administrador
Tras ejecutar el seed de la base de datos, no se crean usuarios por defecto (por seguridad). Para tener un admin:
1. Registrar un usuario con `POST /api/auth/register` (body: username, email, password, cityId; usar un `cityId` existente, p. ej. 1 si corriste el seed).
2. Asignar el rol ADMIN en la tabla `user_role` (insertar `user_id` del nuevo usuario y `role_id` del rol ADMIN), o usar un usuario ya existente con rol ADMIN para crear mas usuarios desde `POST /api/users`.

## Perfil de pruebas
Las pruebas de contexto usan el perfil `test` (`@ActiveProfiles("test")`), por lo que la conexion para tests se toma de `TEST_DB_*` con compatibilidad a `DB_TEST_*`.

## Ejecucion local
Si corres la app desde el IDE o con Maven sin indicar perfil, se usara `dev` como perfil por defecto siempre que tu `.env` tenga:

- `SPRING_PROFILES_DEFAULT=dev`

Comando recomendado:

- `./mvnw spring-boot:run`

## Swagger / OpenAPI
Con la app arriba, la documentacion queda disponible en:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

Para endpoints protegidos, usa `Authorize` en Swagger UI con:

`Bearer <tu_jwt>`
