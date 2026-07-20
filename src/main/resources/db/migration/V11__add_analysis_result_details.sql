-- 문자 원문은 저장하지 않고 해시·마스킹 미리보기·분석 결과만 보관함
ALTER TABLE detection_logs
    ADD COLUMN client_message_id VARCHAR(100),
    ADD COLUMN content_preview VARCHAR(200) NOT NULL DEFAULT '',
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

ALTER TABLE detection_logs
    ALTER COLUMN content_hash TYPE VARCHAR(64),
    ALTER COLUMN sender TYPE VARCHAR(100),
    ALTER COLUMN sms_type TYPE VARCHAR(30),
    ALTER COLUMN detected_at TYPE TIMESTAMP WITH TIME ZONE
        USING detected_at AT TIME ZONE 'Asia/Seoul';

-- 기존 개발 데이터에 해시가 없다면 행 ID 기반의 복구 불가능한 자리표시자로 보완함
UPDATE detection_logs
SET content_hash = LPAD(id::TEXT, 64, '0')
WHERE content_hash IS NULL;
UPDATE detection_logs SET sms_type = 'OTHER' WHERE sms_type IS NULL;
UPDATE detection_logs SET llm_score = 0 WHERE llm_score IS NULL;
UPDATE detection_logs SET url_score = 0 WHERE url_score IS NULL;
UPDATE detection_logs SET pattern_score = 0 WHERE pattern_score IS NULL;
UPDATE detection_logs SET total_score = 0 WHERE total_score IS NULL;
UPDATE detection_logs SET risk_level = 'LOW' WHERE risk_level IS NULL;
UPDATE detection_logs SET ai_explanation = '' WHERE ai_explanation IS NULL;

ALTER TABLE detection_logs
    ALTER COLUMN content_hash SET NOT NULL,
    ALTER COLUMN sms_type SET NOT NULL,
    ALTER COLUMN llm_score SET NOT NULL,
    ALTER COLUMN url_score SET NOT NULL,
    ALTER COLUMN pattern_score SET NOT NULL,
    ALTER COLUMN total_score SET NOT NULL,
    ALTER COLUMN risk_level SET NOT NULL,
    ALTER COLUMN ai_explanation SET NOT NULL;

CREATE UNIQUE INDEX uk_detection_logs_user_client_message
    ON detection_logs(user_id, client_message_id)
    WHERE client_message_id IS NOT NULL;

CREATE TABLE detection_indicators (
    id               BIGSERIAL PRIMARY KEY,
    detection_log_id BIGINT       NOT NULL REFERENCES detection_logs(id) ON DELETE CASCADE,
    indicator_type   VARCHAR(30)  NOT NULL,
    description      VARCHAR(500) NOT NULL
);

CREATE INDEX idx_detection_indicators_log_id
    ON detection_indicators(detection_log_id);

CREATE TABLE detection_url_risks (
    id               BIGSERIAL PRIMARY KEY,
    detection_log_id BIGINT       NOT NULL REFERENCES detection_logs(id) ON DELETE CASCADE,
    original_url     VARCHAR(500) NOT NULL,
    shortened        BOOLEAN      NOT NULL,
    suspicious       BOOLEAN      NOT NULL
);

CREATE INDEX idx_detection_url_risks_log_id
    ON detection_url_risks(detection_log_id);
