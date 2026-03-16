-- Minimal bootstrap data for local/dev use with the current API.
-- Users (admin, cliente_demo) are NOT created here to avoid exposing passwords in the repo.
-- Create the first admin via: POST /api/auth/register with cityId from this seed, then assign ADMIN role in DB if needed.

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
