package com.gold.safefam.domain.statistics.dto;

import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.statistics.enums.StatisticsPeriod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "탐지 이력 대시보드 통계")
public record StatisticsOverviewResponse(
        StatisticsPeriod period,

        @Schema(example = "120")
        long totalAnalysisCount,

        @Schema(example = "27")
        long highRiskCount,

        List<RiskBucket> riskDistribution,

        List<CategoryBucket> categoryDistribution
) {
    public record RiskBucket(RiskLevel riskLevel, long count) {
    }

    public record CategoryBucket(PhishingCategory category, long count) {
    }
}
