-- V3_3_1__gate_proctoring_certs.sql
-- Part 3.3: assessment gates on jobs, light proctoring signals and
-- candidate certifications.

CREATE TABLE job_required_assessments (
    id            char(36) NOT NULL DEFAULT (UUID()),
    job_id        char(36) NOT NULL,
    assessment_id char(36) NOT NULL,
    min_score     int      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_job_required_assessments (job_id, assessment_id),
    KEY idx_job_required_assessments_assessment (assessment_id),
    CONSTRAINT fk_job_required_assessments_job FOREIGN KEY (job_id)
        REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_required_assessments_assessment FOREIGN KEY (assessment_id)
        REFERENCES assessments (id),
    CONSTRAINT chk_job_required_assessments_min CHECK (min_score BETWEEN 1 AND 100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE proctor_events (
    id               char(36)    NOT NULL DEFAULT (UUID()),
    attempt_id       char(36)    NOT NULL,
    type             varchar(20) NOT NULL,
    occurred_at      datetime(3) NOT NULL,
    duration_seconds int         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_proctor_events_attempt (attempt_id, occurred_at),
    CONSTRAINT fk_proctor_events_attempt FOREIGN KEY (attempt_id)
        REFERENCES assessment_attempts (id) ON DELETE CASCADE,
    CONSTRAINT chk_proctor_events_type CHECK (type IN ('hidden', 'blur')),
    CONSTRAINT chk_proctor_events_duration CHECK (duration_seconds >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE certifications (
    id            char(36)     NOT NULL DEFAULT (UUID()),
    user_id       char(36)     NOT NULL,
    name          varchar(255) NOT NULL,
    issuer        varchar(255) NOT NULL,
    issued_on     date         NULL,
    expires_on    date         NULL,
    credential_id varchar(255) NULL,
    PRIMARY KEY (id),
    KEY idx_certifications_user (user_id),
    CONSTRAINT fk_certifications_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;
