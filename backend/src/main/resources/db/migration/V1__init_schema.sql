CREATE TABLE users (
	id CHAR(36) NOT NULL,
	email VARCHAR(320) NOT NULL,
	password_hash VARCHAR(255) NOT NULL,
	role VARCHAR(32) NOT NULL DEFAULT 'ROLE_USER',
	is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
	daily_learning_limit INT NOT NULL DEFAULT 9999,
	created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
	CONSTRAINT pk_users PRIMARY KEY (id),
	CONSTRAINT uk_users_email UNIQUE (email),
	CONSTRAINT ck_users_daily_learning_limit CHECK (daily_learning_limit BETWEEN 1 AND 9999)
);

CREATE TABLE decks (
	id CHAR(36) NOT NULL,
	author_id CHAR(36) NOT NULL,
	name VARCHAR(100) NOT NULL,
	description TEXT NULL,
	cover_image_url VARCHAR(2048) NULL,
	tags TEXT NULL,
	is_public BOOLEAN NOT NULL DEFAULT FALSE,
	created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
	CONSTRAINT pk_decks PRIMARY KEY (id),
	CONSTRAINT fk_decks_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE TABLE cards (
	id CHAR(36) NOT NULL,
	deck_id CHAR(36) NOT NULL,
	front_text TEXT NULL,
	front_media_url VARCHAR(2048) NULL,
	back_text TEXT NULL,
	back_media_url VARCHAR(2048) NULL,
	created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
	CONSTRAINT pk_cards PRIMARY KEY (id),
	CONSTRAINT fk_cards_deck FOREIGN KEY (deck_id) REFERENCES decks (id),
	CONSTRAINT ck_cards_front_content CHECK (
		(front_text IS NOT NULL AND TRIM(front_text) <> '') OR
		(front_media_url IS NOT NULL AND TRIM(front_media_url) <> '')
	),
	CONSTRAINT ck_cards_back_content CHECK (
		(back_text IS NOT NULL AND TRIM(back_text) <> '') OR
		(back_media_url IS NOT NULL AND TRIM(back_media_url) <> '')
	)
);

CREATE TABLE card_learning_states (
	id CHAR(36) NOT NULL,
	card_id CHAR(36) NOT NULL,
	user_id CHAR(36) NOT NULL,
	state VARCHAR(16) NOT NULL DEFAULT 'NEW',
	ease_factor DECIMAL(5,2) NOT NULL DEFAULT 2.50,
	interval_in_days INT NOT NULL DEFAULT 0,
	next_review_date TIMESTAMP(6) NULL,
	version BIGINT NOT NULL DEFAULT 0,
	created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
	CONSTRAINT pk_card_learning_states PRIMARY KEY (id),
	CONSTRAINT uk_learning_state_user_card UNIQUE (user_id, card_id),
	CONSTRAINT fk_learning_states_card FOREIGN KEY (card_id) REFERENCES cards (id),
	CONSTRAINT fk_learning_states_user FOREIGN KEY (user_id) REFERENCES users (id),
	CONSTRAINT ck_learning_states_state CHECK (state IN ('NEW', 'LEARNING', 'MASTERED', 'REVIEW')),
	CONSTRAINT ck_learning_states_interval_non_negative CHECK (interval_in_days >= 0)
);

CREATE INDEX idx_decks_author_id ON decks (author_id);
CREATE INDEX idx_cards_deck_id ON cards (deck_id);
CREATE INDEX idx_learning_states_card_id ON card_learning_states (card_id);
CREATE INDEX idx_learning_states_user_id ON card_learning_states (user_id);
