ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN name VARCHAR(30),
    ADD COLUMN role VARCHAR(20),
    ADD COLUMN deleted_at TIMESTAMP;

UPDATE users
SET password_hash = COALESCE(password_hash, pin_hash, 'MIGRATION_REQUIRED')
WHERE password_hash IS NULL;

UPDATE users
SET name = COALESCE(name, nickname, '사용자')
WHERE name IS NULL;

UPDATE users
SET role = 'USER'
WHERE role IS NULL;

UPDATE users
SET email = CONCAT('user', id, '@placeholder.safefam.local')
WHERE email IS NULL;

ALTER TABLE users
    ALTER COLUMN phone_number DROP NOT NULL,
    ALTER COLUMN pin_hash DROP NOT NULL,
    ALTER COLUMN email SET NOT NULL,
    ALTER COLUMN password_hash SET NOT NULL,
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email),
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));
