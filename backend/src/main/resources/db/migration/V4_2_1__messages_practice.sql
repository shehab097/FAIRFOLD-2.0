-- V4_2_1__messages_practice.sql
-- Part 4.2: application messaging and the isolated interview-practice bank.

CREATE TABLE messages (
    id             char(36)    NOT NULL DEFAULT (UUID()),
    application_id char(36)    NOT NULL,
    sender_id      char(36)    NULL,
    body           text        NULL,
    read_at        datetime(3) NULL,
    created_at     datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_messages_application_created (application_id, created_at),
    CONSTRAINT fk_messages_application FOREIGN KEY (application_id)
        REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE practice_questions (
    id     char(36)     NOT NULL DEFAULT (UUID()),
    text   text         NOT NULL,
    topic  varchar(100) NOT NULL,
    active boolean      NOT NULL DEFAULT true,
    PRIMARY KEY (id),
    KEY idx_practice_questions_topic (topic)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE practice_sessions (
    id             char(36)    NOT NULL DEFAULT (UUID()),
    user_id        char(36)    NOT NULL,
    question_id    char(36)    NOT NULL,
    answer         text        NOT NULL,
    feedback       json        NULL,
    rubric_version varchar(50) NOT NULL DEFAULT 'star-v1',
    provider       varchar(50) NULL,
    rating         smallint    NULL,
    created_at     datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_practice_sessions_user_created (user_id, created_at),
    CONSTRAINT fk_practice_sessions_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_practice_sessions_question FOREIGN KEY (question_id)
        REFERENCES practice_questions (id),
    CONSTRAINT chk_practice_sessions_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;
