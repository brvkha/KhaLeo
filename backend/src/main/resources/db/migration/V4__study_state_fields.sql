ALTER TABLE card_learning_states
    ADD COLUMN last_reviewed_at TIMESTAMP(6) NULL AFTER next_review_date,
    ADD COLUMN learning_step_good_count INT NOT NULL DEFAULT 0 AFTER last_reviewed_at;

ALTER TABLE card_learning_states
    ADD CONSTRAINT ck_learning_step_good_count_non_negative CHECK (learning_step_good_count >= 0);

CREATE INDEX idx_learning_states_state_next_review_date ON card_learning_states (state, next_review_date);
