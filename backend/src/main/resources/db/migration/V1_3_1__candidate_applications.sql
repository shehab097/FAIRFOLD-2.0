-- V1_3_1__candidate_applications.sql
-- Part 1.3: candidate profiles, candidate skills, resumes, applications,
-- application status history and notifications.

CREATE TABLE candidate_profiles (
    user_id                char(36)     NOT NULL,
    phone                  varchar(50)  NULL,
    title                  varchar(255) NULL,
    bio                    text         NULL,
    location               varchar(255) NULL,
    remote_preference      varchar(20)  NOT NULL DEFAULT 'any',
    total_experience_years decimal(4,1) NULL,
    education_level        varchar(20)  NULL,
    previous_job_titles    json         NOT NULL DEFAULT (JSON_ARRAY()),
    updated_at             datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_candidate_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_candidate_profiles_remote CHECK (remote_preference IN ('onsite', 'hybrid', 'remote', 'any')),
    CONSTRAINT chk_candidate_profiles_education CHECK (education_level IN ('high_school', 'diploma', 'bachelor', 'master', 'phd', 'other'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE candidate_skills (
    id          char(36)     NOT NULL DEFAULT (UUID()),
    user_id     char(36)     NOT NULL,
    skill_id    char(36)     NOT NULL,
    level       varchar(20)  NOT NULL,
    years       decimal(3,1) NULL,
    verified    boolean      NOT NULL DEFAULT false,
    verified_at datetime(3)  NULL,
    created_at  datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_candidate_skills_user_skill (user_id, skill_id),
    KEY idx_candidate_skills_skill (skill_id),
    CONSTRAINT fk_candidate_skills_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT chk_candidate_skills_level CHECK (level IN ('beginner', 'intermediate', 'expert'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE resumes (
    id                char(36)     NOT NULL DEFAULT (UUID()),
    user_id           char(36)     NOT NULL,
    original_filename varchar(255) NULL,
    storage_key       varchar(255) NOT NULL,
    size_bytes        bigint       NOT NULL,
    status            varchar(20)  NOT NULL DEFAULT 'uploaded',
    failure_reason    text         NULL,
    stripped_text     text         NULL,
    is_current        boolean      NOT NULL DEFAULT true,
    -- MariaDB has no partial indexes and no CASE/IF in generated columns, so
    -- "one current resume per user" is enforced with a nullable key column:
    -- it holds the user id while current (NULL otherwise) under a UNIQUE index.
    current_user_key  char(36)     NULL,
    created_at        datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_resumes_storage_key (storage_key),
    UNIQUE KEY uq_resumes_current_user (current_user_key),
    KEY idx_resumes_user (user_id),
    CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_resumes_status CHECK (status IN ('uploaded', 'processing', 'ready', 'failed'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE applications (
    id           char(36)    NOT NULL DEFAULT (UUID()),
    job_id       char(36)    NOT NULL,
    candidate_id char(36)    NOT NULL,
    resume_id    char(36)    NOT NULL,
    status       varchar(20) NOT NULL DEFAULT 'applied',
    created_at   datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_applications_job_candidate (job_id, candidate_id),
    KEY idx_applications_job_status (job_id, status),
    KEY idx_applications_candidate (candidate_id),
    CONSTRAINT fk_applications_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_applications_candidate FOREIGN KEY (candidate_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_applications_resume FOREIGN KEY (resume_id) REFERENCES resumes (id),
    CONSTRAINT chk_applications_status CHECK (status IN ('applied', 'screening', 'screened', 'not_matched', 'shortlisted', 'interview', 'hired', 'rejected'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE application_status_history (
    id             char(36)    NOT NULL DEFAULT (UUID()),
    application_id char(36)    NOT NULL,
    from_status    varchar(20) NULL,
    to_status      varchar(20) NOT NULL,
    changed_by     char(36)    NULL,
    reason         text        NULL,
    created_at     datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_status_history_application (application_id),
    CONSTRAINT fk_status_history_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE notifications (
    id         char(36)      NOT NULL DEFAULT (UUID()),
    user_id    char(36)      NOT NULL,
    type       varchar(50)   NOT NULL,
    title      varchar(255)  NOT NULL,
    body       text          NOT NULL,
    link       varchar(2048) NULL,
    read_at    datetime(3)   NULL,
    created_at datetime(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_notifications_user (user_id, read_at, created_at),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;
