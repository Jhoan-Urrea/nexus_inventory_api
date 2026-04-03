ALTER TABLE app_user
ADD COLUMN IF NOT EXISTS activation_token VARCHAR(255);

ALTER TABLE app_user
ADD COLUMN IF NOT EXISTS activation_token_expires_at TIMESTAMPTZ;

ALTER TABLE app_user
ADD COLUMN IF NOT EXISTS activation_required BOOLEAN;

ALTER TABLE app_user
ADD COLUMN IF NOT EXISTS first_login BOOLEAN;

ALTER TABLE app_user
ALTER COLUMN activation_required SET DEFAULT FALSE;

ALTER TABLE app_user
ALTER COLUMN first_login SET DEFAULT FALSE;

UPDATE app_user
SET activation_required = FALSE
WHERE activation_required IS NULL;

UPDATE app_user
SET first_login = FALSE
WHERE first_login IS NULL;

ALTER TABLE app_user
ALTER COLUMN activation_required SET NOT NULL;

ALTER TABLE app_user
ALTER COLUMN first_login SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_activation_token
    ON app_user(activation_token)
    WHERE activation_token IS NOT NULL;
