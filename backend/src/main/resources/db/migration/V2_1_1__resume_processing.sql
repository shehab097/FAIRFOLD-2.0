-- V2_1_1__resume_processing.sql
-- Part 2.1: skill aliases, stripping terms, resume suggestions and placeholder
-- mapping, strip reports, candidate work history and AI call records.

CREATE TABLE skill_aliases (
    id       char(36)     NOT NULL DEFAULT (UUID()),
    skill_id char(36)     NOT NULL,
    alias    varchar(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_skill_aliases_alias (alias),
    KEY idx_skill_aliases_skill (skill_id),
    CONSTRAINT fk_skill_aliases_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE strip_terms (
    id   char(36)     NOT NULL DEFAULT (UUID()),
    kind varchar(20)  NOT NULL,
    term varchar(150) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_strip_terms_kind_term (kind, term),
    CONSTRAINT chk_strip_terms_kind CHECK (kind IN ('institution', 'employer'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE resume_suggestions (
    id        char(36)    NOT NULL DEFAULT (UUID()),
    resume_id char(36)    NOT NULL,
    provider  varchar(20) NOT NULL,
    data      json        NOT NULL,
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_resume_suggestions_resume (resume_id),
    CONSTRAINT fk_resume_suggestions_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE,
    CONSTRAINT chk_resume_suggestions_provider CHECK (provider IN ('ai', 'rule_based'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE resume_placeholder_map (
    id             char(36)    NOT NULL DEFAULT (UUID()),
    resume_id      char(36)    NOT NULL,
    placeholder    varchar(50) NOT NULL,
    kind           varchar(20) NOT NULL,
    original_value text        NOT NULL,
    start_month    date        NULL,
    end_month      date        NULL,
    is_current     boolean     NOT NULL DEFAULT false,
    PRIMARY KEY (id),
    UNIQUE KEY uq_resume_placeholder (resume_id, placeholder),
    KEY idx_resume_placeholder_resume (resume_id),
    CONSTRAINT fk_resume_placeholder_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE,
    CONSTRAINT chk_resume_placeholder_kind CHECK (kind IN ('employer', 'institution'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE strip_reports (
    id         char(36)    NOT NULL DEFAULT (UUID()),
    resume_id  char(36)    NOT NULL,
    user_id    char(36)    NOT NULL,
    note       text        NOT NULL,
    excerpt    text        NULL,
    created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_strip_reports_resume (resume_id),
    CONSTRAINT fk_strip_reports_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE,
    CONSTRAINT fk_strip_reports_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE work_history (
    id          char(36)    NOT NULL DEFAULT (UUID()),
    user_id     char(36)    NOT NULL,
    title       varchar(255) NOT NULL,
    employer    varchar(255) NULL,
    start_month date        NULL,
    end_month   date        NULL,
    is_current  boolean     NOT NULL DEFAULT false,
    skills_used json        NOT NULL DEFAULT (JSON_ARRAY()),
    created_at  datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_work_history_user (user_id),
    CONSTRAINT fk_work_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE ai_calls (
    id                char(36)      NOT NULL DEFAULT (UUID()),
    purpose           varchar(30)   NOT NULL,
    user_id           char(36)      NULL,
    application_id    char(36)      NULL,
    model             varchar(100)  NULL,
    prompt_tokens     int           NOT NULL DEFAULT 0,
    completion_tokens int           NOT NULL DEFAULT 0,
    cost              decimal(10,6) NOT NULL DEFAULT 0,
    status            varchar(20)   NOT NULL,
    created_at        datetime(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ai_calls_created (created_at),
    CONSTRAINT fk_ai_calls_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_calls_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE SET NULL,
    CONSTRAINT chk_ai_calls_purpose CHECK (purpose IN ('profile_extraction', 'screening', 'screening_questions', 'interview_pack', 'practice_feedback', 'story')),
    CONSTRAINT chk_ai_calls_status CHECK (status IN ('ok', 'error', 'blocked'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;
