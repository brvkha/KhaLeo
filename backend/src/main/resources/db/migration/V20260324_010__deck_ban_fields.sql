ALTER TABLE decks
    ADD COLUMN banned_at TIMESTAMP(6) NULL,
    ADD COLUMN banned_by CHAR(36) NULL,
    ADD CONSTRAINT fk_decks_banned_by FOREIGN KEY (banned_by) REFERENCES users (id);

CREATE INDEX idx_decks_banned_at ON decks (banned_at);
