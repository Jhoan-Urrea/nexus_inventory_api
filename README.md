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

## Perfil de pruebas
Las pruebas de contexto usan el perfil `test` (`@ActiveProfiles("test")`), por lo que la conexion para tests se toma de `DB_TEST_*`.
