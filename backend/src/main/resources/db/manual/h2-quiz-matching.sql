-- Manual H2 upgrade on a schema containing Quiz 3B-1 Ordering.
-- Back up and stop the application first. Not automatic; H2 DDL is not atomic.
ALTER TABLE quiz_questions ALTER COLUMN question_type
    ENUM('FILL_IN_BLANK', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'ORDERING', 'MATCHING') NOT NULL;
ALTER TABLE quiz_questions ALTER COLUMN question_type SET DEFAULT 'MULTIPLE_CHOICE';
ALTER TABLE quiz_items ADD COLUMN IF NOT EXISTS item_kind VARCHAR(32) DEFAULT 'ORDERING' NOT NULL;
CREATE TABLE IF NOT EXISTS quiz_question_correct_matches (
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id),
    left_item_id UUID NOT NULL REFERENCES quiz_items(id),
    right_item_id UUID NOT NULL REFERENCES quiz_items(id),
    PRIMARY KEY (question_id, left_item_id),
    UNIQUE (question_id, right_item_id)
);
CREATE TABLE IF NOT EXISTS quiz_answer_matches (
    answer_id BIGINT NOT NULL REFERENCES quiz_answers(id),
    left_item_id UUID NOT NULL REFERENCES quiz_items(id),
    right_item_id UUID NOT NULL REFERENCES quiz_items(id),
    PRIMARY KEY (answer_id, left_item_id),
    UNIQUE (answer_id, right_item_id)
);
