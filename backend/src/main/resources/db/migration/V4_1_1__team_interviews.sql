-- V4_1_1__team_interviews.sql
-- Part 4.1: team invites, interview packs, interviews, interviewers and
-- interview feedback.

ALTER TABLE users
    ADD COLUMN disabled_at datetime(3) NULL;

CREATE TABLE team_invites (
    id          char(36)     NOT NULL DEFAULT (UUID()),
    company_id  char(36)     NOT NULL,
    email       varchar(254) NOT NULL,
    role        varchar(20)  NOT NULL,
    token_hash  char(64)     NOT NULL,
    invited_by  char(36)     NULL,
    expires_at  datetime(3)  NOT NULL,
    accepted_at datetime(3)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_team_invites_token (token_hash),
    KEY idx_team_invites_email (email),
    KEY idx_team_invites_company (company_id),
    CONSTRAINT fk_team_invites_company FOREIGN KEY (company_id)
        REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_invites_invited_by FOREIGN KEY (invited_by)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_team_invites_role CHECK (role IN ('employer_hr', 'employer_manager', 'interviewer'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE interview_packs (
    id          char(36)    NOT NULL DEFAULT (UUID()),
    job_id      char(36)    NOT NULL,
    questions   json        NOT NULL,
    rubric      json        NOT NULL,
    updated_by  char(36)    NULL,
    created_at  datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_interview_packs_job (job_id),
    CONSTRAINT fk_interview_packs_job FOREIGN KEY (job_id)
        REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_interview_packs_updated_by FOREIGN KEY (updated_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE interviews (
    id               char(36)      NOT NULL DEFAULT (UUID()),
    application_id   char(36)      NOT NULL,
    scheduled_at     datetime(3)   NOT NULL,
    duration_minutes int           NOT NULL,
    meeting_link     varchar(2048) NOT NULL,
    created_by       char(36)      NULL,
    PRIMARY KEY (id),
    KEY idx_interviews_scheduled (scheduled_at),
    KEY idx_interviews_application (application_id),
    CONSTRAINT fk_interviews_application FOREIGN KEY (application_id)
        REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_interviews_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_interviews_duration CHECK (duration_minutes BETWEEN 15 AND 240)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE interview_interviewers (
    interview_id char(36) NOT NULL,
    user_id      char(36) NOT NULL,
    PRIMARY KEY (interview_id, user_id),
    CONSTRAINT fk_interview_interviewers_interview FOREIGN KEY (interview_id)
        REFERENCES interviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_interview_interviewers_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE interview_feedback (
    id             char(36)    NOT NULL DEFAULT (UUID()),
    interview_id   char(36)    NOT NULL,
    interviewer_id char(36)    NULL,
    scores         json        NOT NULL DEFAULT (JSON_OBJECT()),
    comments       text        NULL,
    recommendation varchar(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_interview_feedback_interviewer (interview_id, interviewer_id),
    CONSTRAINT fk_interview_feedback_interview FOREIGN KEY (interview_id)
        REFERENCES interviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_interview_feedback_interviewer FOREIGN KEY (interviewer_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_interview_feedback_recommendation
        CHECK (recommendation IN ('strong_yes', 'yes', 'no', 'strong_no'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;
