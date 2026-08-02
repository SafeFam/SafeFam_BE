-- HIGH 위험 분석별 가족 공동 대응 상태와 알림·통화·최종 처리 이력을 저장한다.
-- analysis_id UNIQUE 제약으로 동일 분석 이벤트의 중복 대응 건 생성을 차단한다.
CREATE TABLE family_safety_cases (
    id                       BIGSERIAL PRIMARY KEY,
    analysis_id              BIGINT       NOT NULL UNIQUE REFERENCES detection_logs(id) ON DELETE CASCADE,
    ward_id                  BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status                   VARCHAR(30)  NOT NULL,
    risk_score               INTEGER      NOT NULL,
    suspected_institution    VARCHAR(100) NOT NULL,
    risky_action             VARCHAR(500) NOT NULL,
    masked_message_preview   VARCHAR(200) NOT NULL,
    masked_risk_summary      VARCHAR(1000) NOT NULL,
    detected_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    first_notified_at        TIMESTAMP WITH TIME ZONE,
    last_notified_at         TIMESTAMP WITH TIME ZONE,
    next_reminder_at         TIMESTAMP WITH TIME ZONE,
    reminder_count           INTEGER      NOT NULL DEFAULT 0,
    called_by                BIGINT REFERENCES users(id) ON DELETE SET NULL,
    called_at                TIMESTAMP WITH TIME ZONE,
    handled_by               BIGINT REFERENCES users(id) ON DELETE SET NULL,
    resolved_at              TIMESTAMP WITH TIME ZONE,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_family_safety_status
        CHECK (status IN ('PENDING', 'CONTACTING', 'SAFE_CONFIRMED', 'TRANSFERRED')),
    CONSTRAINT chk_family_safety_risk_score CHECK (risk_score BETWEEN 0 AND 100)
);

-- 보호 대상별 최신 대응 건 목록 조회를 지원한다.
CREATE INDEX idx_family_safety_cases_ward_created
    ON family_safety_cases(ward_id, created_at DESC);

-- 스케줄러가 미해결 상태 중 재알림 시각이 지난 건만 빠르게 조회하도록 지원한다.
CREATE INDEX idx_family_safety_cases_due
    ON family_safety_cases(next_reminder_at)
    WHERE status IN ('PENDING', 'CONTACTING');
