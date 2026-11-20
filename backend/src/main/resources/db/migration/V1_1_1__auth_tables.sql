-- V1_1_1__auth_tables.sql
-- Part 1.1: companies, users, refresh tokens, password reset tokens, audit log.

CREATE TABLE companies (
    id                 char(36)     NOT NULL DEFAULT (UUID()),
    name               varchar(255) NULL,
    industry           varchar(255) NULL,
    size               varchar(20)  NULL,
    show_name_publicly boolean      NOT NULL DEFAULT false,
    created_at         datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at         datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT chk_companies_size CHECK (size IN ('1-10', '11-50', '51-200', '201-1000', '1000+'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE users (
    id                 char(36)     NOT NULL DEFAULT (UUID()),
    email              varchar(254) NOT NULL,
    password_hash      varchar(255) NOT NULL,
    first_name         varchar(255) NOT NULL,
    last_name          varchar(255) NOT NULL,
    role               varchar(20)  NOT NULL,
    company_id         char(36)     NULL,
    email_verified     boolean      NOT NULL DEFAULT true,
    failed_login_count int          NOT NULL DEFAULT 0,
    locked_until       datetime(3)  NULL,
    consent_at         datetime(3)  NOT NULL,
    created_at         datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at         datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE SET NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('candidate', 'employer_hr', 'employer_manager', 'interviewer', 'admin'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE refresh_tokens (
    id         char(36)     NOT NULL DEFAULT (UUID()),
    user_id    char(36)     NOT NULL,
    token_hash char(64)     NOT NULL,
    expires_at datetime(3)  NOT NULL,
    revoked_at datetime(3)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_token_hash (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE password_reset_tokens (
    id         char(36)     NOT NULL DEFAULT (UUID()),
    user_id    char(36)     NOT NULL,
    token_hash char(64)     NOT NULL,
    expires_at datetime(3)  NOT NULL,
    used_at    datetime(3)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_reset_token_hash (token_hash),
    CONSTRAINT fk_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE audit_log (
    id            char(36)     NOT NULL DEFAULT (UUID()),
    actor_id      char(36)     NULL,
    action        varchar(100) NOT NULL,
    resource_type varchar(100) NOT NULL,
    resource_id   varchar(255) NULL,
    details       json         NOT NULL DEFAULT (JSON_OBJECT()),
    created_at    datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_audit_created_at (created_at),
    KEY idx_audit_actor (actor_id),
    KEY idx_audit_action (action)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;
