-- V4_3_2__assessment_generation_purpose.sql
-- Allow the new assessment-generation AI purpose in ai_calls.

ALTER TABLE ai_calls
    DROP CONSTRAINT chk_ai_calls_purpose;

ALTER TABLE ai_calls
    ADD CONSTRAINT chk_ai_calls_purpose CHECK (
        purpose IN ('profile_extraction', 'screening', 'screening_questions',
                    'interview_pack', 'practice_feedback', 'story', 'assessment_generation')
    );
