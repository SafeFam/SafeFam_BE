/**
 * 화이트리스트·익명 신고·월간 트렌드 카드 기능을 위한 스키마를 구성한다.
 *
 * 화이트리스트에는 사용자별 발신자 유일성을 적용하고, 신고 테이블에서는 사용자 식별자를
 * 제거한 뒤 보호된 분석 스냅샷만 유지한다. 트렌드 키워드는 개인정보가 포함될 수 있는
 * 임의 토큰 대신 서버 허용 목록에서 추출한 표준 위험 키워드만 저장한다.
 */

-- 화이트리스트 발신자 정규화와 사용자별 중복 방지를 지원함
ALTER TABLE whitelist
    ALTER COLUMN sender TYPE VARCHAR(100),
    ADD COLUMN label VARCHAR(50),
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
        USING created_at AT TIME ZONE 'Asia/Seoul';

DELETE FROM whitelist newer
USING whitelist older
WHERE newer.user_id = older.user_id
  AND newer.sender = older.sender
  AND newer.id > older.id;

CREATE UNIQUE INDEX uk_whitelist_user_sender
    ON whitelist(user_id, sender);

-- 신고 시 사용자 식별자를 제거하고 분석 결과의 비식별 스냅샷만 저장함
ALTER TABLE reports
    ALTER COLUMN user_id DROP NOT NULL,
    ALTER COLUMN report_type TYPE VARCHAR(30),
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
        USING created_at AT TIME ZONE 'Asia/Seoul',
    ADD COLUMN sms_type VARCHAR(30),
    ADD COLUMN risk_level VARCHAR(10),
    ADD COLUMN content_hash VARCHAR(64),
    ADD COLUMN content_preview VARCHAR(200);

UPDATE reports report
SET sms_type = detection.sms_type,
    risk_level = detection.risk_level,
    content_hash = detection.content_hash,
    content_preview = detection.content_preview
FROM detection_logs detection
WHERE report.detection_log_id = detection.id;

UPDATE reports
SET user_id = NULL,
    report_type = COALESCE(report_type, 'PHISHING'),
    sms_type = COALESCE(sms_type, 'OTHER'),
    risk_level = COALESCE(risk_level, 'LOW'),
    content_hash = COALESCE(content_hash, LPAD(id::TEXT, 64, '0')),
    content_preview = COALESCE(content_preview, '');

ALTER TABLE reports
    ALTER COLUMN report_type SET NOT NULL,
    ALTER COLUMN sms_type SET NOT NULL,
    ALTER COLUMN risk_level SET NOT NULL,
    ALTER COLUMN content_hash SET NOT NULL,
    ALTER COLUMN content_preview SET NOT NULL;

ALTER TABLE reports
    DROP CONSTRAINT IF EXISTS reports_detection_log_id_fkey;

ALTER TABLE reports
    ADD CONSTRAINT reports_detection_log_id_fkey
        FOREIGN KEY (detection_log_id)
        REFERENCES detection_logs(id)
        ON DELETE SET NULL;

CREATE UNIQUE INDEX uk_reports_detection_log
    ON reports(detection_log_id)
    WHERE detection_log_id IS NOT NULL;

CREATE INDEX idx_detection_logs_trend_month
    ON detection_logs(detected_at, risk_level, sms_type);

-- 임의 토큰 대신 개인정보가 없는 서버 허용 위험 키워드만 저장함
CREATE TABLE detection_keywords (
    id               BIGSERIAL PRIMARY KEY,
    detection_log_id BIGINT      NOT NULL
        REFERENCES detection_logs(id) ON DELETE CASCADE,
    keyword          VARCHAR(50) NOT NULL,
    CONSTRAINT uk_detection_keywords_log_keyword
        UNIQUE (detection_log_id, keyword)
);

CREATE INDEX idx_detection_keywords_keyword
    ON detection_keywords(keyword);
