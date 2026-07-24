package com.gold.safefam.domain.report.dto;

import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.report.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/** 저장된 익명 신고의 식별자와 비식별 분석 분류 결과를 반환한다. */
@Schema(description = "익명 신고 저장 결과")
public record ReportResponse(
        Long reportId,
        Long analysisId,
        ReportType type,
        PhishingCategory category,
        RiskLevel riskLevel,
        OffsetDateTime reportedAt
) {
}
