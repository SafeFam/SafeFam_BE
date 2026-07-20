/**
 * 분석 이력마다 하나의 피드백만 저장하고 분석 삭제 시 피드백도 함께 제거한다.
 */
CREATE TABLE analysis_feedbacks (
    id               BIGSERIAL PRIMARY KEY,
    detection_log_id BIGINT       NOT NULL UNIQUE
        REFERENCES detection_logs(id) ON DELETE CASCADE,
    feedback_type    VARCHAR(30)  NOT NULL,
    comment          VARCHAR(500),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analysis_feedbacks_log_id
    ON analysis_feedbacks(detection_log_id);
