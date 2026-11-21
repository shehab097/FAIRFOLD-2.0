-- V1_6_1__job_salary.sql
-- Salary model: fixed amount, range, or negotiable (no amount).

ALTER TABLE jobs
    ADD COLUMN salary_type     varchar(20)  NOT NULL DEFAULT 'negotiable' AFTER remote,
    ADD COLUMN salary_min      decimal(12,2) NULL AFTER salary_type,
    ADD COLUMN salary_max      decimal(12,2) NULL AFTER salary_min,
    ADD COLUMN salary_currency char(3)      NOT NULL DEFAULT 'USD' AFTER salary_max,
    ADD COLUMN salary_period   varchar(10)  NOT NULL DEFAULT 'year' AFTER salary_currency,
    ADD CONSTRAINT chk_jobs_salary_type CHECK (salary_type IN ('fixed', 'range', 'negotiable')),
    ADD CONSTRAINT chk_jobs_salary_period CHECK (salary_period IN ('year', 'month', 'hour')),
    ADD CONSTRAINT chk_jobs_salary_amounts CHECK (
        (salary_type = 'negotiable' AND salary_min IS NULL AND salary_max IS NULL)
        OR (salary_type = 'fixed' AND salary_min IS NOT NULL AND salary_max IS NULL AND salary_min >= 0)
        OR (salary_type = 'range' AND salary_min IS NOT NULL AND salary_max IS NOT NULL
            AND salary_min >= 0 AND salary_max >= salary_min)
    );
