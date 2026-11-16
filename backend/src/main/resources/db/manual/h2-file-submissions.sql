-- One-time H2 development schema upgrade. NOT executed automatically by Spring.
-- Back up the existing database and stop the application before applying manually.
-- Run against the existing schema before starting the FILE-capable application.
-- Preserves IDs, payloads, timestamps and history; never drops tables or data.
-- H2 DDL commits implicitly: these statements are not an atomic transaction.
ALTER TABLE submissions ALTER COLUMN payload DROP NOT NULL;
ALTER TABLE submissions ALTER COLUMN submission_type ENUM ('FILE', 'GIT_URL', 'TEXT') NOT NULL;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS file_bucket VARCHAR(63);
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS file_object_key VARCHAR(1024);
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS file_original_filename VARCHAR(255);
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS file_media_type VARCHAR(127);
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS file_sha256 VARCHAR(64);
