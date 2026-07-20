CREATE TABLE devices (
                         id          BIGSERIAL PRIMARY KEY,
                         user_id     BIGINT       NOT NULL REFERENCES users(id),
                         fcm_token   VARCHAR(255) NOT NULL,
                         platform    VARCHAR(20)  NOT NULL,
                         created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_devices_user_id ON devices(user_id);