-- Runs once, only on first init of this container's data volume (see 01-init-accounts-db.sql for
-- the same convention). Adds a database for identification-service's drafts of the sign-up
-- wizard's Personal + Contact + Identification step - separate from "accounts" (real, approved
-- accounts) and from kyc-service (which owns no database of its own; it verifies over HTTP).
CREATE USER identification_user WITH PASSWORD 'identification_pass';
CREATE DATABASE identification OWNER identification_user;
