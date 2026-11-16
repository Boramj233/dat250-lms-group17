-- Manual H2 upgrade; back up the database and stop the app before applying.
-- Never run automatically. Existing submissions remain individual; no history is rewritten.
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS group_id BIGINT;
ALTER TABLE submissions ADD CONSTRAINT IF NOT EXISTS fk_submission_group
    FOREIGN KEY (group_id) REFERENCES groups(id);
CREATE TABLE IF NOT EXISTS submission_members (
    submission_id BIGINT NOT NULL REFERENCES submissions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    enrollment_id BIGINT NOT NULL REFERENCES enrollments(id),
    CONSTRAINT uq_submission_member UNIQUE (submission_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_submission_group_content ON submissions(content_id, group_id, submitted_at, id);
CREATE INDEX IF NOT EXISTS idx_submission_member_user ON submission_members(user_id, submission_id);
