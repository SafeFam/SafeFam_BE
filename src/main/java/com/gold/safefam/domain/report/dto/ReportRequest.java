package com.gold.safefam.domain.report.dto;

import com.gold.safefam.domain.report.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 사용자가 탐지 이력을 어떤 유형으로 신고하는지 전달한다. */
@Schema(description = "탐지 이력 신고 요청")
public record ReportRequest(
        @NotNull
        @Schema(example = "PHISHING")
        ReportType type
) {
}
