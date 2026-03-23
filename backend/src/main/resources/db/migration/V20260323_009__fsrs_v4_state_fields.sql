ALTER TABLE card_learning_states
    ADD COLUMN fsrs_stability DECIMAL(10,4) NOT NULL DEFAULT 0.0000 AFTER learning_step_good_count,
    ADD COLUMN fsrs_difficulty DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER fsrs_stability,
    ADD COLUMN fsrs_elapsed_days INT NOT NULL DEFAULT 0 AFTER fsrs_difficulty,
    ADD COLUMN fsrs_scheduled_days INT NOT NULL DEFAULT 0 AFTER fsrs_elapsed_days,
    ADD COLUMN fsrs_reps INT NOT NULL DEFAULT 0 AFTER fsrs_scheduled_days,
    ADD COLUMN fsrs_lapses INT NOT NULL DEFAULT 0 AFTER fsrs_reps;

ALTER TABLE card_learning_states
    DROP CHECK ck_learning_states_state,
    ADD CONSTRAINT ck_learning_states_state CHECK (state IN ('NEW', 'LEARNING', 'RELEARNING', 'MASTERED', 'REVIEW')),
    ADD CONSTRAINT ck_learning_states_fsrs_elapsed_non_negative CHECK (fsrs_elapsed_days >= 0),
    ADD CONSTRAINT ck_learning_states_fsrs_scheduled_non_negative CHECK (fsrs_scheduled_days >= 0),
    ADD CONSTRAINT ck_learning_states_fsrs_reps_non_negative CHECK (fsrs_reps >= 0),
    ADD CONSTRAINT ck_learning_states_fsrs_lapses_non_negative CHECK (fsrs_lapses >= 0);
