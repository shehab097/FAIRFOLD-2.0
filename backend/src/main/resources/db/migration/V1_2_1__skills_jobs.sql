-- V1_2_1__skills_jobs.sql
-- Part 1.2: skills, jobs and job_skills.

CREATE TABLE skills (
    id         char(36)     NOT NULL DEFAULT (UUID()),
    name       varchar(100) NOT NULL,
    created_at datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_skills_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE jobs (
    id               char(36)     NOT NULL DEFAULT (UUID()),
    company_id       char(36)     NOT NULL,
    created_by       char(36)     NULL,
    title            varchar(255) NOT NULL,
    description      text         NOT NULL,
    requirements     text         NOT NULL DEFAULT (''),
    experience_level varchar(20)  NOT NULL,
    city             varchar(255) NULL,
    country          varchar(255) NOT NULL,
    remote           boolean      NOT NULL DEFAULT false,
    status           varchar(20)  NOT NULL DEFAULT 'draft',
    hf_min_years     int          NULL,
    hf_country       varchar(255) NULL,
    cutoff_score     int          NULL,
    published_at     datetime(3)  NULL,
    closed_at        datetime(3)  NULL,
    created_at       datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       datetime(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_jobs_company_status (company_id, status),
    KEY idx_jobs_status_published (status, published_at),
    CONSTRAINT fk_jobs_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_jobs_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_jobs_experience_level CHECK (experience_level IN ('entry', 'mid', 'senior', 'lead')),
    CONSTRAINT chk_jobs_status CHECK (status IN ('draft', 'active', 'paused', 'closed')),
    CONSTRAINT chk_jobs_cutoff CHECK (cutoff_score IS NULL OR (cutoff_score >= 0 AND cutoff_score <= 100)),
    FULLTEXT KEY ft_jobs_search (title, description, requirements)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;

CREATE TABLE job_skills (
    id        char(36)    NOT NULL DEFAULT (UUID()),
    job_id    char(36)    NOT NULL,
    skill_id  char(36)    NOT NULL,
    kind      varchar(20) NOT NULL,
    must_have boolean     NOT NULL DEFAULT false,
    PRIMARY KEY (id),
    UNIQUE KEY uq_job_skills (job_id, skill_id),
    KEY idx_job_skills_skill (skill_id),
    CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
    CONSTRAINT chk_job_skills_kind CHECK (kind IN ('required', 'nice_to_have')),
    CONSTRAINT chk_job_skills_must_have CHECK (must_have = false OR kind = 'required')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_as_ci;
