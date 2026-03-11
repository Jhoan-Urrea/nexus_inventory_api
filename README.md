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

## Perfil de pruebas
Las pruebas de contexto usan el perfil `test` (`@ActiveProfiles("test")`), por lo que la conexion para tests se toma de `DB_TEST_*`.

## Swagger / OpenAPI
Con la app arriba, la documentación queda disponible en:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

Para endpoints protegidos, usa `Authorize` en Swagger UI con:

`Bearer <tu_jwt>`
