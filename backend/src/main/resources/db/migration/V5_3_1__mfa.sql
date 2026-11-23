-- Part 5.3: TOTP MFA columns. Secrets are AES-256-GCM encrypted by the app.
ALTER TABLE users
    ADD COLUMN mfa_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN mfa_secret_enc text NULL,
    ADD COLUMN mfa_pending_secret_enc text NULL,
    ADD COLUMN mfa_last_step bigint NULL;
