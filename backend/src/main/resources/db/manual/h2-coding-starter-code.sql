-- Manual H2 upgrade: back up and stop the application first.
-- Not executed automatically. H2 DDL is not atomic.
-- Existing exercises retain null starter code; repository URLs are unchanged.
ALTER TABLE coding_exercises ADD COLUMN IF NOT EXISTS starter_code VARCHAR(10000);
