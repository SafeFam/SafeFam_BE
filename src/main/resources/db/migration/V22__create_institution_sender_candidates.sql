CREATE TABLE institution_sender_candidates (
    id                 BIGSERIAL    PRIMARY KEY,
    institution        VARCHAR(100) NOT NULL,
    normalized_sender  VARCHAR(20)  NOT NULL,
    observation_count  INTEGER      NOT NULL DEFAULT 1,
    status             VARCHAR(30)  NOT NULL,
    first_seen_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    last_analysis_id   BIGINT       NOT NULL,
    reviewed_at        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_institution_sender_candidate
        UNIQUE (institution, normalized_sender),
    CONSTRAINT chk_sender_candidate_observation_count
        CHECK (observation_count > 0),
    CONSTRAINT chk_sender_candidate_status
        CHECK (status IN (
            'COLLECTING',
            'REVIEW_REQUIRED',
            'APPROVED',
            'REJECTED'
        ))
);

CREATE INDEX idx_sender_candidates_review_queue
    ON institution_sender_candidates(status, last_seen_at DESC);
