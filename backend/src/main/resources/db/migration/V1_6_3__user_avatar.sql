-- V1_6_3__user_avatar.sql
-- Optional profile photo for candidates and employer HR users.

ALTER TABLE users
    ADD COLUMN avatar_url varchar(512) NULL AFTER company_id;
