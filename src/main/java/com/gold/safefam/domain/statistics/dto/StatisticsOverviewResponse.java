package com.gold.safefam.domain.statistics.dto;

import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.statistics.enums.StatisticsPeriod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 선택 기간의 개인 탐지 건수와 위험 등급·피싱 유형별 분포를 전달한다. */
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
    /** 하나의 위험 등급에 해당하는 탐지 건수를 전달한다. */
    public record RiskBucket(RiskLevel riskLevel, long count) {
    }

    /** 하나의 피싱 유형에 해당하는 탐지 건수를 전달한다. */
    public record CategoryBucket(PhishingCategory category, long count) {
    }
}
