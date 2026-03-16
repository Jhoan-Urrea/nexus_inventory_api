# nexus_inventory_api
Proyecto academico para la asignatura de Software III.

## Variables de entorno
La aplicacion carga variables desde `.env` usando:

`spring.config.import=optional:file:.env[.properties]`

Variables minimas requeridas:

- `DB_DEV_URL`
- `DB_DEV_USER`
- `DB_DEV_PASSWORD`
- `DB_TEST_URL`
- `DB_TEST_USER`
- `DB_TEST_PASSWORD`
- `jwt_secret`
- `jwt_expiration`
- `JWT_TEST_SECRET`
- `JWT_TEST_EXPIRATION`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_PROTOCOL`

## Primer usuario administrador
Tras ejecutar el seed de la base de datos, no se crean usuarios por defecto (por seguridad). Para tener un admin:
1. Registrar un usuario con `POST /api/auth/register` (body: username, email, password, cityId; usar un `cityId` existente, p. ej. 1 si corriste el seed).
2. Asignar el rol ADMIN en la tabla `user_role` (insertar `user_id` del nuevo usuario y `role_id` del rol ADMIN), o usar un usuario ya existente con rol ADMIN para crear más usuarios desde `POST /api/users`.

## Perfil de pruebas
Las pruebas de contexto usan el perfil `test` (`@ActiveProfiles("test")`), por lo que la conexion para tests se toma de `DB_TEST_*`.

## Swagger / OpenAPI
Con la app arriba, la documentación queda disponible en:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

Para endpoints protegidos, usa `Authorize` en Swagger UI con:

`Bearer <tu_jwt>`
