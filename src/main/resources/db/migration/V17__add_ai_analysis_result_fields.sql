ALTER TABLE detection_logs
    ADD COLUMN raw_text_score INTEGER,
    ADD COLUMN raw_url_score INTEGER,
    ADD COLUMN raw_rules_score INTEGER,
    ADD COLUMN failure_code VARCHAR(100);

ALTER TABLE detection_url_risks
    ADD COLUMN traced_url VARCHAR(500),
    ADD COLUMN malicious BOOLEAN,
    ADD COLUMN risk_score INTEGER,
    ADD COLUMN engine_source VARCHAR(100),
    ADD COLUMN error_code VARCHAR(200);

ALTER TABLE detection_logs
    ADD CONSTRAINT chk_detection_raw_text_score
        CHECK (raw_text_score BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_detection_raw_url_score
        CHECK (raw_url_score BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_detection_raw_rules_score
        CHECK (raw_rules_score BETWEEN 0 AND 100);

CREATE TABLE processed_analysis_events (
     event_id UUID PRIMARY KEY,
     analysis_id BIGINT NOT NULL,
     event_type VARCHAR(30) NOT NULL,
     processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_processed_analysis_events_analysis_id
    ON processed_analysis_events(analysis_id);
