ALTER TABLE users
    ADD COLUMN banned_at TIMESTAMP(6) NULL,
    ADD COLUMN banned_by CHAR(36) NULL,
    ADD CONSTRAINT fk_users_banned_by FOREIGN KEY (banned_by) REFERENCES users (id);

CREATE TABLE admin_moderation_actions (
    id CHAR(36) NOT NULL,
    admin_user_id CHAR(36) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id CHAR(36) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason_code VARCHAR(128) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_admin_moderation_actions PRIMARY KEY (id),
    CONSTRAINT fk_admin_actions_admin_user FOREIGN KEY (admin_user_id) REFERENCES users (id)
);

CREATE INDEX idx_admin_moderation_actions_admin_user_id ON admin_moderation_actions (admin_user_id);
CREATE INDEX idx_admin_moderation_actions_target ON admin_moderation_actions (target_type, target_id);
