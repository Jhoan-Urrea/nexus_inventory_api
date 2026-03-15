-- Minimal bootstrap data for local/dev use with the current API.
-- The admin password is inserted in plain text on purpose so that
-- PasswordMigrationRunner can encode it automatically in the dev profile.

BEGIN;

INSERT INTO role (name, description)
VALUES
    ('ADMIN', 'Platform administrator'),
    ('WAREHOUSE_EMPLOYEE', 'Warehouse operational user'),
    ('WAREHOUSE_SUPERVISOR', 'Warehouse supervisor user'),
    ('USER', 'Standard authenticated user (legacy)'),
    ('SALES_AGENT', 'Commercial user'),
    ('CLIENT', 'Client role')
ON CONFLICT (name) DO NOTHING;

INSERT INTO country (country_name, country_description)
SELECT 'Colombia', 'Default country for local bootstrap'
WHERE NOT EXISTS (
    SELECT 1
    FROM country
    WHERE country_name = 'Colombia'
);

INSERT INTO department_region (d_region_name, d_region_description, country_id)
SELECT 'Cundinamarca', 'Default region for local bootstrap', c.country_id
FROM country c
WHERE c.country_name = 'Colombia'
  AND NOT EXISTS (
      SELECT 1
      FROM department_region
      WHERE d_region_name = 'Cundinamarca'
  );

INSERT INTO city (city_name, city_description, postal_code, d_region_id)
SELECT 'Bogota', 'Default city for local bootstrap', '110111', r.d_region_id
FROM department_region r
WHERE r.d_region_name = 'Cundinamarca'
  AND NOT EXISTS (
      SELECT 1
      FROM city
      WHERE city_name = 'Bogota'
  );

INSERT INTO client (
    name,
    email,
    phone,
    document_type,
    document_number,
    business_name,
    address,
    status
)
SELECT
    'Cliente Demo',
    'cliente.demo@nexus.local',
    '3000000000',
    'NIT',
    '900000001',
    'Cliente Demo SAS',
    'Bogota',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM client
    WHERE email = 'cliente.demo@nexus.local'
);

INSERT INTO app_user (username, email, password, status, city_id, client_id)
SELECT
    'admin',
    'admin@nexus.local',
    'Admin123',
    'ACTIVE',
    c.city_id,
    NULL
FROM city c
WHERE c.city_name = 'Bogota'
  AND NOT EXISTS (
      SELECT 1
      FROM app_user
      WHERE email = 'admin@nexus.local'
  )
LIMIT 1;

INSERT INTO app_user (username, email, password, status, city_id, client_id)
SELECT
    'cliente_demo',
    'cliente.usuario@nexus.local',
    'Cliente123',
    'ACTIVE',
    c.city_id,
    cl.id
FROM city c
CROSS JOIN client cl
WHERE c.city_name = 'Bogota'
  AND cl.email = 'cliente.demo@nexus.local'
  AND NOT EXISTS (
      SELECT 1
      FROM app_user
      WHERE email = 'cliente.usuario@nexus.local'
  )
LIMIT 1;

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN role r ON r.name = 'ADMIN'
WHERE u.email = 'admin@nexus.local'
ON CONFLICT DO NOTHING;

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN role r ON r.name = 'CLIENT'
WHERE u.email = 'cliente.usuario@nexus.local'
ON CONFLICT DO NOTHING;

INSERT INTO warehouse (
    name,
    description,
    capacity,
    available_capacity_m2,
    total_capacity_m2,
    location,
    active,
    city_id
)
SELECT
    'Bodega Central',
    'Bodega inicial para desarrollo local',
    250.00,
    250.00,
    250.00,
    'Bogota',
    TRUE,
    c.city_id
FROM city c
WHERE c.city_name = 'Bogota'
  AND NOT EXISTS (
      SELECT 1
      FROM warehouse
      WHERE name = 'Bodega Central'
  )
LIMIT 1;

COMMIT;
