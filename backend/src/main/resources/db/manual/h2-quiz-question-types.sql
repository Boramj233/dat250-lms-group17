-- Manual H2 upgrade: back up and stop the application first. Not executed automatically.
-- H2 DDL is not atomic. Old questions remain multiple choice; old answers are preserved.
ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS question_type VARCHAR(32) DEFAULT 'MULTIPLE_CHOICE' NOT NULL;
ALTER TABLE quiz_answers ADD COLUMN IF NOT EXISTS text_answer VARCHAR(255);
CREATE TABLE IF NOT EXISTS quiz_accepted_answers (
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id),
    answer_text VARCHAR(255) NOT NULL,
    UNIQUE(question_id, answer_text)
);
