-- V2_2_1__screening.sql
-- Part 2.2: candidate aliases, screening runs/items, application claims and
-- the screening result fields on applications.

CREATE TABLE candidate_aliases (
    id           char(36)    NOT NULL DEFAULT (UUID()),
    company_id   char(36)    NOT NULL,
    candidate_id char(36)    NOT NULL,
    alias        varchar(20) NOT NULL,
    created_at   datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_candidate_aliases_company_candidate (company_id, candidate_id),
    UNIQUE KEY uq_candidate_aliases_company_alias (company_id, alias),
    KEY idx_candidate_aliases_candidate (candidate_id),
    CONSTRAINT fk_candidate_aliases_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_aliases_candidate FOREIGN KEY (candidate_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE screening_runs (
    id             char(36)      NOT NULL DEFAULT (UUID()),
    job_id         char(36)      NOT NULL,
    started_by     char(36)      NULL,
    mode           varchar(20)   NOT NULL,
    total          int           NOT NULL DEFAULT 0,
    done_count     int           NOT NULL DEFAULT 0,
    failed_count   int           NOT NULL DEFAULT 0,
    estimated_cost decimal(10,4) NOT NULL DEFAULT 0,
    status         varchar(20)   NOT NULL DEFAULT 'running',
    finished_at    datetime(3)   NULL,
    created_at     datetime(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     datetime(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_screening_runs_job (job_id, created_at),
    CONSTRAINT fk_screening_runs_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_screening_runs_started_by FOREIGN KEY (started_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_screening_runs_mode CHECK (mode IN ('ai', 'rule_based')),
    CONSTRAINT chk_screening_runs_status CHECK (status IN ('running', 'finished'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE screening_items (
    id             char(36)    NOT NULL DEFAULT (UUID()),
    run_id         char(36)    NOT NULL,
    application_id char(36)    NOT NULL,
    status         varchar(20) NOT NULL DEFAULT 'queued',
    error          text        NULL,
    attempts       int         NOT NULL DEFAULT 0,
    skip_filters   boolean     NOT NULL DEFAULT false,
    created_at     datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_screening_items_run_application (run_id, application_id),
    KEY idx_screening_items_run_status (run_id, status),
    KEY idx_screening_items_application (application_id),
    CONSTRAINT fk_screening_items_run FOREIGN KEY (run_id) REFERENCES screening_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_screening_items_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT chk_screening_items_status CHECK (status IN ('queued', 'running', 'done', 'failed'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE application_claims (
    id               char(36)    NOT NULL DEFAULT (UUID()),
    application_id   char(36)    NOT NULL,
    requirement_key  varchar(100) NOT NULL,
    requirement_text text        NOT NULL,
    kind             varchar(20) NOT NULL,
    verdict          varchar(20) NOT NULL,
    quote            text        NULL,
    weight           int         NOT NULL,
    source           varchar(20) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_application_claims_application (application_id),
    CONSTRAINT fk_application_claims_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT chk_application_claims_kind CHECK (kind IN ('required', 'nice_to_have', 'experience')),
    CONSTRAINT chk_application_claims_verdict CHECK (verdict IN ('met', 'partial', 'not_met')),
    CONSTRAINT chk_application_claims_source CHECK (source IN ('ai', 'rule_based'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

ALTER TABLE applications
    ADD COLUMN score int NULL,
    ADD COLUMN scoring_mode varchar(20) NULL,
    ADD COLUMN bias_status varchar(20) NULL,
    ADD COLUMN bias_note text NULL,
    ADD COLUMN missing_skills json NOT NULL DEFAULT (JSON_ARRAY()),
    ADD COLUMN not_matched_rule text NULL,
    ADD COLUMN pulled_back_at datetime(3) NULL,
    ADD COLUMN pulled_back_by char(36) NULL,
    ADD COLUMN shortlisted_at datetime(3) NULL,
    ADD COLUMN decided_by char(36) NULL,
    ADD COLUMN decision_reason text NULL,
    ADD COLUMN override_direction varchar(30) NOT NULL DEFAULT 'none',
    ADD COLUMN gate_status varchar(20) NOT NULL DEFAULT 'not_required',
    ADD CONSTRAINT fk_applications_pulled_back_by FOREIGN KEY (pulled_back_by) REFERENCES users (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_applications_decided_by FOREIGN KEY (decided_by) REFERENCES users (id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_applications_score CHECK (score BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_applications_scoring_mode CHECK (scoring_mode IN ('ai', 'rule_based')),
    ADD CONSTRAINT chk_applications_bias_status CHECK (bias_status IN ('clear', 'flagged')),
    ADD CONSTRAINT chk_applications_override_direction CHECK (override_direction IN ('none', 'shortlisted_below_cutoff', 'rejected_above_cutoff')),
    ADD CONSTRAINT chk_applications_gate_status CHECK (gate_status IN ('not_required', 'pending', 'passed', 'failed')),
    ADD CONSTRAINT chk_applications_override_reason CHECK (
        override_direction = 'none'
        OR (decision_reason IS NOT NULL AND CHAR_LENGTH(TRIM(decision_reason)) >= 10)
    ),
    ADD KEY idx_applications_job_score (job_id, score);
