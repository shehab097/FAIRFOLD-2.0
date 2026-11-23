-- V4_3_1__assessment_pool_queue.sql
-- CV-driven assessment topics, a 30-question pool and per-attempt random draws.

ALTER TABLE assessments
    ADD COLUMN questions_per_attempt int NOT NULL DEFAULT 10,
    ADD COLUMN review_status varchar(20) NOT NULL DEFAULT 'approved',
    ADD CONSTRAINT chk_assessments_questions_per_attempt
        CHECK (questions_per_attempt BETWEEN 1 AND 50),
    ADD CONSTRAINT chk_assessments_review_status
        CHECK (review_status IN ('pending_generation', 'pending_review', 'approved', 'ignored'));

ALTER TABLE assessment_attempts
    ADD COLUMN question_ids json NOT NULL DEFAULT (JSON_ARRAY());
