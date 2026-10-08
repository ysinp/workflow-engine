-- Runs once, only on first init of this container's data volume (see
-- 01-init-accounts-db.sql for the same convention). Adds a second application
-- database, separate from "accounts", for temporal-orchestration-workflow-engine's
-- persisted Workflow Editor diagrams (see api.WorkflowDefinitionController) - these
-- are workflow definitions, not account rows, so they don't belong in "accounts".
CREATE USER workflow_engine_user WITH PASSWORD 'workflow_engine_pass';
CREATE DATABASE workflow_engine OWNER workflow_engine_user;
