CREATE TABLE detection_evidence_cards (
    id                BIGSERIAL    PRIMARY KEY,
    detection_log_id  BIGINT       NOT NULL,
    category          VARCHAR(50)  NOT NULL,
    title             VARCHAR(100) NOT NULL,
    description       VARCHAR(500) NOT NULL,
    CONSTRAINT fk_detection_evidence_cards_log
        FOREIGN KEY (detection_log_id)
            REFERENCES detection_logs(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_detection_evidence_cards_log_id
    ON detection_evidence_cards(detection_log_id);
