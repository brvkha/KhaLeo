CREATE TABLE media_upload_authorizations (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    max_size_bytes BIGINT NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    issued_at TIMESTAMP(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    rejection_reason VARCHAR(128) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_media_upload_authorizations PRIMARY KEY (id),
    CONSTRAINT fk_media_upload_authorizations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_media_upload_authorizations_status CHECK (
        status IN ('ISSUED', 'EXPIRED', 'REJECTED_TYPE', 'REJECTED_SIZE', 'REJECTED_RATE_LIMIT')
    ),
    CONSTRAINT ck_media_upload_authorizations_max_size_positive CHECK (max_size_bytes > 0)
);

CREATE TABLE media_object_references (
    object_key VARCHAR(512) NOT NULL,
    reference_count INT NOT NULL,
    last_referenced_at TIMESTAMP(6) NOT NULL,
    last_dereferenced_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_media_object_references PRIMARY KEY (object_key),
    CONSTRAINT ck_media_object_references_count_non_negative CHECK (reference_count >= 0)
);

CREATE INDEX idx_media_upload_authorizations_user_id ON media_upload_authorizations (user_id);
CREATE INDEX idx_media_upload_authorizations_expires_at ON media_upload_authorizations (expires_at);
