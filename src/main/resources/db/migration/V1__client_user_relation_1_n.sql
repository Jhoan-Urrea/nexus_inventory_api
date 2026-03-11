-- Migración: Client 1 -> N AppUser
-- Ejecutar sobre la base de datos donde ya existe la tabla client con user_id.
-- Si la tabla client no existe, créala primero con: CREATE TABLE client (id BIGSERIAL PRIMARY KEY, name VARCHAR(255));

-- Eliminar relación antigua
ALTER TABLE client
DROP COLUMN IF EXISTS user_id;

-- Agregar nueva relación
ALTER TABLE app_user
ADD COLUMN client_id BIGINT;

-- Crear foreign key
ALTER TABLE app_user
ADD CONSTRAINT fk_app_user_client
FOREIGN KEY (client_id)
REFERENCES client(id)
ON DELETE SET NULL;

-- Crear índice
CREATE INDEX idx_app_user_client
ON app_user(client_id);
