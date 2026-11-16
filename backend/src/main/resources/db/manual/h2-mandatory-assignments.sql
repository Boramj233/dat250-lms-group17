-- Manual H2 upgrade: back up the database and stop the application first.
-- Not run automatically. H2 DDL is not atomic. Existing assignments default to false.
ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS mandatory_for_exam BOOLEAN DEFAULT FALSE NOT NULL;
