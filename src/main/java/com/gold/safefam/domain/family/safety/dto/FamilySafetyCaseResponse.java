package com.gold.safefam.domain.family.safety.dto;

import com.gold.safefam.domain.family.safety.entity.FamilySafetyCase;
import com.gold.safefam.domain.family.safety.enums.FamilySafetyStatus;

import java.time.OffsetDateTime;

/**
 * 보호자 화면에 표시할 가족 공동 대응 건의 상세 응답이다.
 * 문자 원문 대신 생성 시점에 마스킹한 미리보기와 위험 요약만 반환한다.
 */
public record FamilySafetyCaseResponse(
        Long caseId,
        Long analysisId,
        Long wardId,
        String wardName,
        FamilySafetyStatus status,
        int riskScore,
        String suspectedInstitution,
        String riskyAction,
        String maskedMessagePreview,
        String riskSummary,
        OffsetDateTime detectedAt,
        OffsetDateTime firstNotifiedAt,
        OffsetDateTime lastNotifiedAt,
        OffsetDateTime nextReminderAt,
        int reminderCount,
        Long calledById,
        String calledByName,
        OffsetDateTime calledAt,
        Long handledById,
        String handledByName,
        OffsetDateTime resolvedAt
) {
    /** 영속 엔티티를 보호자 API 응답 형태로 변환한다. */
    public static FamilySafetyCaseResponse from(FamilySafetyCase safetyCase) {
        return new FamilySafetyCaseResponse(
                safetyCase.getId(),
                safetyCase.getAnalysis().getId(),
                safetyCase.getWard().getId(),
                safetyCase.getWard().getName(),
                safetyCase.getStatus(),
                safetyCase.getRiskScore(),
                safetyCase.getSuspectedInstitution(),
                safetyCase.getRiskyAction(),
                safetyCase.getMaskedMessagePreview(),
                safetyCase.getMaskedRiskSummary(),
                safetyCase.getDetectedAt(),
                safetyCase.getFirstNotifiedAt(),
                safetyCase.getLastNotifiedAt(),
                safetyCase.getNextReminderAt(),
                safetyCase.getReminderCount(),
                safetyCase.getCalledBy() != null ? safetyCase.getCalledBy().getId() : null,
                safetyCase.getCalledBy() != null ? safetyCase.getCalledBy().getName() : null,
                safetyCase.getCalledAt(),
                safetyCase.getHandledBy() != null ? safetyCase.getHandledBy().getId() : null,
                safetyCase.getHandledBy() != null ? safetyCase.getHandledBy().getName() : null,
                safetyCase.getResolvedAt()
        );
    }
}
