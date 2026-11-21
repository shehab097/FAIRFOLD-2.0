-- V1_5_1__job_address.sql
-- Optional street address for a job location, alongside city and country.

ALTER TABLE jobs
    ADD COLUMN address varchar(255) NULL AFTER city;
