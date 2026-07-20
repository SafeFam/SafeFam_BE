package com.gold.safefam.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 탐지·알림 설정")
public record UserSettingsResponse(
        @Schema(example = "true")
        boolean autoAnalysisEnabled,

        @Schema(example = "true")
        boolean pushEnabled
) {
}
