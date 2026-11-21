-- V1_4_1__company_details.sql
-- Richer company profile. `about` and `benefits` are Markdown.

ALTER TABLE companies
    ADD COLUMN tagline       varchar(160)  NULL AFTER name,
    ADD COLUMN about         text          NULL,
    ADD COLUMN website       varchar(2048) NULL,
    ADD COLUMN logo_url      varchar(2048) NULL,
    ADD COLUMN linkedin_url  varchar(2048) NULL,
    ADD COLUMN founded_year  smallint      NULL,
    ADD COLUMN headquarters  varchar(255)  NULL,
    ADD COLUMN contact_email varchar(254)  NULL,
    ADD COLUMN phone         varchar(40)   NULL,
    ADD COLUMN benefits      text          NULL,
    ADD CONSTRAINT chk_companies_founded_year CHECK (founded_year IS NULL OR founded_year BETWEEN 1800 AND 2100);

