-- Manual H2 upgrade: back up and stop the application before applying.
-- Not executed automatically. H2 DDL is not atomic. Existing question/answer tables are unchanged.
-- Support both Hibernate's native H2 ENUM and the earlier manual VARCHAR column.
ALTER TABLE quiz_questions ALTER COLUMN question_type
    ENUM('FILL_IN_BLANK', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'ORDERING') NOT NULL;
ALTER TABLE quiz_questions ALTER COLUMN question_type SET DEFAULT 'MULTIPLE_CHOICE';
CREATE TABLE IF NOT EXISTS quiz_items (
    id UUID PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id),
    item_text VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS quiz_question_correct_order (
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id),
    item_id UUID NOT NULL REFERENCES quiz_items(id),
    order_position INTEGER NOT NULL CHECK (order_position >= 0),
    PRIMARY KEY (question_id, order_position),
    UNIQUE (question_id, item_id)
);
CREATE TABLE IF NOT EXISTS quiz_answer_ordered_items (
    answer_id BIGINT NOT NULL REFERENCES quiz_answers(id),
    item_id UUID NOT NULL REFERENCES quiz_items(id),
    order_position INTEGER NOT NULL CHECK (order_position >= 0),
    PRIMARY KEY (answer_id, order_position),
    UNIQUE (answer_id, item_id)
);
