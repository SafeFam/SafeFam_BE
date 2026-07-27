-- 비동기 분석 처리 상태를 추가
ALTER TABLE detection_logs
    ADD COLUMN analysis_status VARCHAR(30);

-- 기존 데이터는 이미 분석 결과가 저장된 데이터이므로 완료 상태로 처리
UPDATE detection_logs
SET analysis_status = 'COMPLETED';

ALTER TABLE detection_logs
    ALTER COLUMN analysis_status SET NOT NULL;

-- PENDING 상태에서는 분석 결과가 아직 없으므로 결과 필드를 nullable로 변경
ALTER TABLE detection_logs
    ALTER COLUMN sms_type DROP NOT NULL,
    ALTER COLUMN llm_score DROP NOT NULL,
    ALTER COLUMN url_score DROP NOT NULL,
    ALTER COLUMN pattern_score DROP NOT NULL,
    ALTER COLUMN total_score DROP NOT NULL,
    ALTER COLUMN risk_level DROP NOT NULL,
    ALTER COLUMN ai_explanation DROP NOT NULL,
    ALTER COLUMN detected_at DROP NOT NULL,
    ALTER COLUMN detected_at DROP DEFAULT;

-- 상태별 조회와 Outbox 후속 처리에 사용할 인덱스
CREATE INDEX idx_detection_logs_analysis_status
    ON detection_logs(analysis_status);
