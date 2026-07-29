package com.gold.safefam.domain.analysis.event;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import com.gold.safefam.domain.analysis.enums.RiskLevel;

/* 분석 결과 DB 커밋 완료 내부 이벤트 */
public record AnalysisResultCommittedEvent(
        Long analysisId,
        Long userId,
        AnalysisStatus status,
        RiskLevel riskLevel,
        String explanation
) {

    /* Analysis 엔티티 기반 정적 팩토리 메서드 */
    public static AnalysisResultCommittedEvent from(
            Analysis analysis
    ) {
        return new AnalysisResultCommittedEvent(
                analysis.getId(),
                analysis.getUserId(),
                analysis.getStatus(),
                analysis.getRiskLevel(),
                analysis.getExplanation()
        );
    }

    // 실패 알림 분기물
    public boolean isFailed() {
        return status == AnalysisStatus.FAILED;
    }

    // 긴급 푸시 알림 분기물
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH;
    }
}