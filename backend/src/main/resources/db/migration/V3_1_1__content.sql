-- V3_1_1__content.sql
-- Part 3.1: admin-managed assessments, their multiple-choice questions and
-- learning resources. Assessments and resources are deactivated, never deleted.
-- The attempts table is created here because question locking and attempt
-- counts in this part depend on it (see D-57 in docs/progress.md).

CREATE TABLE assessments (
    id               char(36)    NOT NULL DEFAULT (UUID()),
    title            text        NOT NULL,
    skill_id         char(36)    NOT NULL,
    duration_minutes int         NOT NULL,
    pass_mark        int         NOT NULL,
    active           boolean     NOT NULL DEFAULT true,
    created_at       datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_assessments_skill (skill_id),
    CONSTRAINT fk_assessments_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT chk_assessments_duration CHECK (duration_minutes BETWEEN 5 AND 180),
    CONSTRAINT chk_assessments_pass_mark CHECK (pass_mark BETWEEN 1 AND 100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE assessment_questions (
    id                char(36)    NOT NULL DEFAULT (UUID()),
    assessment_id     char(36)    NOT NULL,
    position          int         NOT NULL,
    text              text        NOT NULL,
    options           json        NOT NULL,
    correct_option_id varchar(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_assessment_questions_position (assessment_id, position),
    CONSTRAINT fk_assessment_questions_assessment FOREIGN KEY (assessment_id)
        REFERENCES assessments (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE resources (
    id              char(36)     NOT NULL DEFAULT (UUID()),
    skill_id        char(36)     NOT NULL,
    title           text         NOT NULL,
    url             varchar(2048) NOT NULL,
    cost            varchar(10)  NOT NULL DEFAULT 'free',
    language        varchar(50)  NOT NULL DEFAULT 'English',
    level           varchar(20)  NOT NULL,
    active          boolean      NOT NULL DEFAULT true,
    broken          boolean      NOT NULL DEFAULT false,
    last_checked_at datetime(3)  NULL,
    created_at      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_resources_skill (skill_id),
    CONSTRAINT fk_resources_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT chk_resources_cost CHECK (cost IN ('free', 'paid')),
    CONSTRAINT chk_resources_level CHECK (level IN ('beginner', 'intermediate', 'advanced'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE assessment_attempts (
    id                 char(36)    NOT NULL DEFAULT (UUID()),
    assessment_id      char(36)    NOT NULL,
    user_id            char(36)    NOT NULL,
    started_at         datetime(3) NOT NULL,
    ends_at            datetime(3) NOT NULL,
    submitted_at       datetime(3) NULL,
    answers            json        NOT NULL DEFAULT (JSON_OBJECT()),
    score              int         NULL,
    passed             boolean     NULL,
    status             varchar(20) NOT NULL DEFAULT 'in_progress',
    proctoring_consent boolean     NULL,
    explanation        text        NULL,
    PRIMARY KEY (id),
    KEY idx_attempts_user_assessment (user_id, assessment_id, started_at),
    KEY idx_attempts_status_ends (status, ends_at),
    KEY idx_attempts_assessment (assessment_id),
    CONSTRAINT fk_attempts_assessment FOREIGN KEY (assessment_id) REFERENCES assessments (id),
    CONSTRAINT fk_attempts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_attempts_status CHECK (status IN ('in_progress', 'submitted', 'auto_submitted')),
    CONSTRAINT chk_attempts_score CHECK (score BETWEEN 0 AND 100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;
