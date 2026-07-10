package com.gold.safefam.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 탐지·알림 설정 변경 요청")
public record UpdateUserSettingsRequest(
        @Schema(example = "true")
        Boolean autoAnalysisEnabled,

        @Schema(example = "true")
        Boolean pushEnabled,

        @Schema(description = "분석 후 원문 저장 여부", example = "false")
        Boolean saveMessageContent
) {
}
