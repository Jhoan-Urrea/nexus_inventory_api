-- Eliminación crítica: tabla ya no usada por la API (logout solo revoca refresh token).
-- Ejecutar una vez si la tabla existía en la BD (Hibernate con ddl-auto=update no borra tablas).
-- Uso: psql $DB_URL -f database/drop_auth_revoked_token.sql

DROP TABLE IF EXISTS auth_revoked_token;
