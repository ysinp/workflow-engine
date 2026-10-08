-- Runs once, only on first init of this container's data volume (official
-- postgres image convention: everything in /docker-entrypoint-initdb.d/ runs
-- as POSTGRES_USER the first time the volume is empty). Temporal's own
-- "temporal"/"temporal_visibility" databases are created separately by the
-- temporalio/auto-setup container against this same server - this script
-- only adds the application's "accounts" database and role alongside them.
CREATE USER accounts_user WITH PASSWORD 'accounts_pass';
CREATE DATABASE accounts OWNER accounts_user;
