package com.gold.safefam.domain.family.safety.dto;

/**
 * 트랜잭션이 끝난 뒤 FCM을 보내는 데 필요한 최소 정보만 담는 내부 전달 객체다.
 * JPA 엔티티를 비동기 알림 단계까지 직접 전달하지 않도록 경계를 분리한다.
 */
public record FamilySafetyNotificationTarget(
        Long caseId,
        Long analysisId,
        Long wardId,
        String wardName,
        int riskScore,
        String riskyAction
) {
}
