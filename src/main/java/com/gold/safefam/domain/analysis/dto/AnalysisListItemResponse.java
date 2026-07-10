package com.gold.safefam.domain.analysis.dto;

import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "탐지 이력 목록 항목")
public record AnalysisListItemResponse(
        @Schema(example = "101")
        Long analysisId,

        @Schema(example = "1588****")
        String maskedSender,

        @Schema(example = "대출 승인 완료. 아래 링크에서...")
        String messagePreview,

        @Schema(example = "92")
        int riskScore,

        RiskLevel riskLevel,

        PhishingCategory category,

        OffsetDateTime analyzedAt
) {
}
