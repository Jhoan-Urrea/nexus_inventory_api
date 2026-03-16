-- Align an existing PostgreSQL schema with the current Spring/JPA model.
-- Intended to be executed after the base storage schema.

BEGIN;

-- ------------------------------------------------------------
-- Geography: adapt clean schema names to the names expected by JPA
-- ------------------------------------------------------------

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'region'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'department_region'
    ) THEN
        ALTER TABLE region RENAME TO department_region;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'country'
          AND column_name = 'id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'country'
          AND column_name = 'country_id'
    ) THEN
        ALTER TABLE country RENAME COLUMN id TO country_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'country'
          AND column_name = 'name'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'country'
          AND column_name = 'country_name'
    ) THEN
        ALTER TABLE country RENAME COLUMN name TO country_name;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'country'
          AND column_name = 'description'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'country'
          AND column_name = 'country_description'
    ) THEN
        ALTER TABLE country RENAME COLUMN description TO country_description;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'department_region'
          AND column_name = 'id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'department_region'
          AND column_name = 'd_region_id'
    ) THEN
        ALTER TABLE department_region RENAME COLUMN id TO d_region_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'department_region'
          AND column_name = 'name'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'department_region'
          AND column_name = 'd_region_name'
    ) THEN
        ALTER TABLE department_region RENAME COLUMN name TO d_region_name;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'department_region'
          AND column_name = 'description'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'department_region'
          AND column_name = 'd_region_description'
    ) THEN
        ALTER TABLE department_region RENAME COLUMN description TO d_region_description;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'city'
          AND column_name = 'id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'city'
          AND column_name = 'city_id'
    ) THEN
        ALTER TABLE city RENAME COLUMN id TO city_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'city'
          AND column_name = 'name'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'city'
          AND column_name = 'city_name'
    ) THEN
        ALTER TABLE city RENAME COLUMN name TO city_name;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'city'
          AND column_name = 'description'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'city'
          AND column_name = 'city_description'
    ) THEN
        ALTER TABLE city RENAME COLUMN description TO city_description;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'city'
          AND column_name = 'region_id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'city'
          AND column_name = 'd_region_id'
    ) THEN
        ALTER TABLE city RENAME COLUMN region_id TO d_region_id;
    END IF;
END $$;

-- ------------------------------------------------------------
-- Enum values expected by the application
-- ------------------------------------------------------------

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_enum e ON e.enumtypid = t.oid
        WHERE t.typname = 'user_status'
          AND e.enumlabel = 'SUSPENDED'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_enum e ON e.enumtypid = t.oid
        WHERE t.typname = 'user_status'
          AND e.enumlabel = 'BLOCKED'
    ) THEN
        ALTER TYPE user_status RENAME VALUE 'SUSPENDED' TO 'BLOCKED';
    END IF;
END $$;

-- ------------------------------------------------------------
-- Warehouse columns required by the current API
-- ------------------------------------------------------------

ALTER TABLE warehouse
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS capacity NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS active BOOLEAN;

UPDATE warehouse
SET active = TRUE
WHERE active IS NULL;

ALTER TABLE warehouse
    ALTER COLUMN active SET DEFAULT TRUE;

-- ------------------------------------------------------------
-- Auth persistence tables required by current services
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS auth_refresh_token (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    token VARCHAR(700) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_token_email
    ON auth_refresh_token (email);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_token_expires_at
    ON auth_refresh_token (expires_at);

CREATE TABLE IF NOT EXISTS auth_revoked_token (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    token VARCHAR(700) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auth_revoked_token_expires_at
    ON auth_revoked_token (expires_at);

CREATE TABLE IF NOT EXISTS auth_password_reset_token (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE auth_password_reset_token
    ADD COLUMN IF NOT EXISTS code VARCHAR(6);

ALTER TABLE auth_password_reset_token
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;

UPDATE auth_password_reset_token
SET code = '000000',
    used = TRUE
WHERE code IS NULL;

ALTER TABLE auth_password_reset_token
    ALTER COLUMN code SET NOT NULL;

ALTER TABLE auth_password_reset_token
    DROP CONSTRAINT IF EXISTS auth_password_reset_token_token_key;

ALTER TABLE auth_password_reset_token
    DROP COLUMN IF EXISTS token;

CREATE INDEX IF NOT EXISTS idx_auth_password_reset_token_email
    ON auth_password_reset_token (email);

CREATE INDEX IF NOT EXISTS idx_auth_password_reset_token_email_code
    ON auth_password_reset_token (email, code);

CREATE INDEX IF NOT EXISTS idx_auth_password_reset_token_expires_at
    ON auth_password_reset_token (expires_at);

CREATE TABLE IF NOT EXISTS auth_audit_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(100),
    details VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auth_audit_log_event_type
    ON auth_audit_log (event_type);

CREATE INDEX IF NOT EXISTS idx_auth_audit_log_created_at
    ON auth_audit_log (created_at);

COMMIT;
