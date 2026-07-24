package com.gold.safefam.domain.statistics.dto;

import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.YearMonth;
import java.util.List;

/** 월간 트렌드 카드에 필요한 표본 수·피싱 유형 순위·위험 키워드 순위를 전달한다. */
@Schema(description = "월간 금융 사기 트렌드 카드")
public record TrendCardResponse(
        @Schema(example = "2026-07")
        YearMonth month,

        @Schema(description = "집계에 포함된 중·고위험 익명 탐지 건수")
        long sampleSize,

        List<PhishingTypeTrend> topPhishingTypes,

        List<RiskKeywordTrend> topRiskKeywords
) {
    /** 월간 중·고위험 탐지 건수를 피싱 유형별 순위로 표현한다. */
    public record PhishingTypeTrend(
            int rank,
            PhishingCategory category,
            long count
    ) {
    }

    /** 개인정보가 없는 표준 위험 키워드의 월간 출현 순위를 표현한다. */
    public record RiskKeywordTrend(
            int rank,
            String keyword,
            long count
    ) {
    }
}
