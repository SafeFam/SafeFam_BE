package com.gold.safefam.domain.family.safety.dto;

/**
 * HIGH 분석에 대한 대응 건 생성 결과다.
 * created 값으로 중복 이벤트에서는 최초 FCM을 다시 보내지 않도록 판단한다.
 */
public record FamilySafetyCaseCreationResult(
        FamilySafetyNotificationTarget target,
        boolean created
) {
}
