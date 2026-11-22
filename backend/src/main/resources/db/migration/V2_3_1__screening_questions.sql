-- V2_3_1__screening_questions.sql
-- Part 2.3: employer-editable screening questions per job.

CREATE TABLE job_screening_questions (
    id       char(36)    NOT NULL DEFAULT (UUID()),
    job_id   char(36)    NOT NULL,
    position int         NOT NULL,
    text     text        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_job_screening_questions_position (job_id, position),
    CONSTRAINT fk_job_screening_questions_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;
