-- V14__add_family_protection_columns.sql

ALTER TABLE family_links
    ADD COLUMN qr_token   VARCHAR(64) UNIQUE,
    ADD COLUMN expires_at TIMESTAMP,
    ADD COLUMN linked_at  TIMESTAMP,
    ALTER COLUMN status   SET NOT NULL,
    ALTER COLUMN status   SET DEFAULT 'PENDING',
    ADD CONSTRAINT chk_family_links_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REVOKED'));

ALTER TABLE family_links
    ADD CONSTRAINT uq_family_links_invite_code UNIQUE (invite_code);

ALTER TABLE family_links
    ALTER COLUMN invite_code TYPE VARCHAR(6),
    ALTER COLUMN protected_id DROP NOT NULL;