DROP PROCEDURE IF EXISTS migrate_rich_card_content;

DELIMITER $$
CREATE PROCEDURE migrate_rich_card_content()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'cards'
          AND column_name = 'image_url'
    ) THEN
        ALTER TABLE cards ADD COLUMN image_url VARCHAR(2048) NULL AFTER back_media_url;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'cards'
          AND column_name = 'part_of_speech'
    ) THEN
        ALTER TABLE cards ADD COLUMN part_of_speech VARCHAR(64) NULL AFTER image_url;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'cards'
          AND column_name = 'phonetic'
    ) THEN
        ALTER TABLE cards ADD COLUMN phonetic VARCHAR(255) NULL AFTER part_of_speech;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'cards'
          AND column_name = 'examples_json'
    ) THEN
        ALTER TABLE cards ADD COLUMN examples_json TEXT NOT NULL AFTER phonetic;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'cards'
          AND column_name = 'version'
    ) THEN
        ALTER TABLE cards ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER examples_json;
    END IF;

    UPDATE cards
       SET image_url = COALESCE(image_url, front_media_url, back_media_url)
     WHERE image_url IS NULL;

    UPDATE cards
       SET examples_json = '[]'
     WHERE examples_json IS NULL OR TRIM(examples_json) = '';
END$$
DELIMITER ;

CALL migrate_rich_card_content();
DROP PROCEDURE IF EXISTS migrate_rich_card_content;
