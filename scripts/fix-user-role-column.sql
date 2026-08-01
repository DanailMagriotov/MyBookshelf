-- Expands user.role so MASTER_ADMIN (12 chars) fits. Run once on existing databases.
ALTER TABLE user MODIFY COLUMN role VARCHAR(20) NOT NULL;
