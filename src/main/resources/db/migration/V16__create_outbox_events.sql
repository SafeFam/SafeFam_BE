CREATE TABLE outbox_events (
    id                BIGSERIAL PRIMARY KEY,
    event_id          UUID         NOT NULL UNIQUE,
    aggregate_type    VARCHAR(50)  NOT NULL,
    aggregate_id      BIGINT       NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    schema_version    VARCHAR(20)  NOT NULL,
    encrypted_payload TEXT         NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    attempts          INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at      TIMESTAMP WITH TIME ZONE,
    last_error        VARCHAR(500)
);

CREATE INDEX idx_outbox_events_publish
    ON outbox_events(status, next_attempt_at);

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events(aggregate_type, aggregate_id);