CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE identity.account (
                                  id            UUID         PRIMARY KEY,
                                  email         VARCHAR(254) NOT NULL,
                                  handle        VARCHAR(30)  NOT NULL,
                                  password_hash VARCHAR(100) NOT NULL,
                                  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                                  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                  CONSTRAINT uq_account_email  UNIQUE (email),
                                  CONSTRAINT uq_account_handle UNIQUE (handle),
                                  CONSTRAINT ck_account_email_lower CHECK (email = lower(email)),
                                  CONSTRAINT ck_account_handle_format CHECK (handle ~ '^[a-z0-9_]{3,30}$')
    );

CREATE TABLE identity.refresh_token (
                                        id          UUID        PRIMARY KEY,
                                        account_id  UUID        NOT NULL REFERENCES identity.account (id) ON DELETE CASCADE,
                                        family_id   UUID        NOT NULL,
                                        token_hash  VARCHAR(64)    NOT NULL,
                                        expires_at  TIMESTAMPTZ NOT NULL,
                                        revoked_at  TIMESTAMPTZ,
                                        created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_account ON identity.refresh_token (account_id);
CREATE INDEX idx_refresh_token_family  ON identity.refresh_token (family_id);