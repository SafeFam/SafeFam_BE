package com.gold.safefam.domain.family.safety.dto;

/**
 * 보호자 권한 확인과 통화 시도 기록을 마친 뒤 반환하는 응답이다.
 * 앱은 phoneNumber를 이용해 실제 tel 통화를 실행한다.
 */
public record FamilyCallResponse(
        Long caseId,
        Long wardId,
        String wardName,
        String phoneNumber
) {
}
