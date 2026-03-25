CREATE TABLE IF NOT EXISTS auth_revoked_access_token (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_revoked_access_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_revoked_access_token_hash
    ON auth_revoked_access_token(token_hash);

CREATE INDEX IF NOT EXISTS idx_revoked_access_token_email
    ON auth_revoked_access_token(email);

CREATE INDEX IF NOT EXISTS idx_revoked_access_token_expires
    ON auth_revoked_access_token(expires_at);
