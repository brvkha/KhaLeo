CREATE TABLE deck_import_links (
    id CHAR(36) NOT NULL,
    source_deck_id CHAR(36) NOT NULL,
    target_private_deck_id CHAR(36) NOT NULL,
    imported_by_user_id CHAR(36) NOT NULL,
    last_imported_at TIMESTAMP(6) NOT NULL,
    last_merge_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_deck_import_links PRIMARY KEY (id),
    CONSTRAINT fk_deck_import_source FOREIGN KEY (source_deck_id) REFERENCES decks (id),
    CONSTRAINT fk_deck_import_target FOREIGN KEY (target_private_deck_id) REFERENCES decks (id),
    CONSTRAINT uk_deck_import_source_target UNIQUE (source_deck_id, target_private_deck_id),
    CONSTRAINT ck_deck_import_last_merge_status CHECK (
        last_merge_status IN ('SUCCESS', 'CONFLICT_REQUIRED', 'FAILED')
    )
);

CREATE TABLE reimport_merge_conflicts (
    id CHAR(36) NOT NULL,
    deck_import_link_id CHAR(36) NOT NULL,
    conflict_scope VARCHAR(32) NOT NULL,
    target_entity_id VARCHAR(64) NOT NULL,
    field_path VARCHAR(255) NOT NULL,
    local_value_snapshot TEXT NULL,
    cloud_value_snapshot TEXT NULL,
    resolution_choice VARCHAR(16) NULL,
    resolved_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reimport_merge_conflicts PRIMARY KEY (id),
    CONSTRAINT fk_reimport_conflict_link FOREIGN KEY (deck_import_link_id) REFERENCES deck_import_links (id),
    CONSTRAINT ck_reimport_conflict_scope CHECK (
        conflict_scope IN ('DECK_FIELD', 'CARD_ITEM', 'MEDIA_REF')
    ),
    CONSTRAINT ck_reimport_resolution_choice CHECK (
        resolution_choice IS NULL OR resolution_choice IN ('LOCAL', 'CLOUD')
    )
);

CREATE INDEX idx_deck_import_target_private ON deck_import_links (target_private_deck_id);
CREATE INDEX idx_reimport_conflict_link ON reimport_merge_conflicts (deck_import_link_id);
