-- Part 5.1: indexes used by admin user search and audit filtering.
ALTER TABLE users
    ADD KEY idx_users_role (role);

ALTER TABLE audit_log
    ADD KEY idx_audit_resource_type (resource_type);
