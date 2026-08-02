package com.gold.safefam.domain.family.safety.dto;

import com.gold.safefam.domain.family.safety.enums.FamilySafetyStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 보호자가 공동 대응 건을 SAFE_CONFIRMED 또는 TRANSFERRED로
 * 최종 처리할 때 사용하는 상태 변경 요청이다.
 */
public record FamilySafetyStatusRequest(
        @NotNull FamilySafetyStatus status
) {
}
