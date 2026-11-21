-- V1_6_2__job_salary_negotiable.sql
-- Whether a fixed/range salary can be negotiated. Negotiable-type jobs are always negotiable.

ALTER TABLE jobs
    ADD COLUMN salary_negotiable boolean NOT NULL DEFAULT true AFTER salary_period;
