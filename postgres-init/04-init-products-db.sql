-- Runs once, only on first init of this container's data volume (see 01-init-accounts-db.sql for
-- the same convention). Adds a database for product-service's banking product catalog, shown on
-- the sign-up wizard's product-selection screen.
CREATE USER products_user WITH PASSWORD 'products_pass';
CREATE DATABASE products OWNER products_user;
