package com.gold.safefam.domain.statistics.service;

import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.statistics.dto.StatisticsOverviewResponse;
import com.gold.safefam.domain.statistics.dto.StatisticsOverviewResponse.CategoryBucket;
import com.gold.safefam.domain.statistics.dto.StatisticsOverviewResponse.RiskBucket;
import com.gold.safefam.domain.statistics.dto.TrendCardResponse;
import com.gold.safefam.domain.statistics.dto.TrendCardResponse.PhishingTypeTrend;
import com.gold.safefam.domain.statistics.dto.TrendCardResponse.RiskKeywordTrend;
import com.gold.safefam.domain.statistics.enums.StatisticsPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 인증 사용자의 탐지 이력을 기간별로 집계해 대시보드 응답으로 변환한다. */
@RequiredArgsConstructor
@Service
public class StatisticsService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<RiskLevel> TREND_RISK_LEVELS = List.of(
            RiskLevel.MEDIUM,
            RiskLevel.HIGH
    );

    private final AnalysisRepository analysisRepository;

    /** 선택 기간과 사용자 ID를 기준으로 전체·고위험·분포 통계를 생성한다. */
    @Transactional(readOnly = true)
    public StatisticsOverviewResponse getOverview(Long userId, StatisticsPeriod period) {
        OffsetDateTime fromAt = resolveFromAt(period, OffsetDateTime.now(ZoneOffset.UTC));

        Map<RiskLevel, Long> riskCounts = initializeRiskCounts();
        findRiskCounts(userId, fromAt).forEach(result ->
                riskCounts.put(result.getRiskLevel(), result.getCount())
        );

        Map<PhishingCategory, Long> categoryCounts = initializeCategoryCounts();
        findCategoryCounts(userId, fromAt).forEach(result ->
                categoryCounts.put(result.getCategory(), result.getCount())
        );

        long totalAnalysisCount = riskCounts.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        long highRiskCount = riskCounts.get(RiskLevel.HIGH);

        List<RiskBucket> riskDistribution = Arrays.stream(RiskLevel.values())
                .map(riskLevel -> new RiskBucket(riskLevel, riskCounts.get(riskLevel)))
                .toList();
        List<CategoryBucket> categoryDistribution = Arrays.stream(PhishingCategory.values())
                .map(category -> new CategoryBucket(category, categoryCounts.get(category)))
                .toList();

        return new StatisticsOverviewResponse(
                period,
                totalAnalysisCount,
                highRiskCount,
                riskDistribution,
                categoryDistribution
        );
    }

    /** 월간 익명 탐지를 집계해 피싱 유형 Top 3와 표준 위험 키워드 Top 5를 반환한다. */
    @Transactional(readOnly = true)
    public TrendCardResponse getTrendCards(String requestedMonth) {
        YearMonth month = requestedMonth == null
                ? YearMonth.now(SERVICE_ZONE)
                : YearMonth.parse(requestedMonth);
        OffsetDateTime fromAt = month.atDay(1).atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
        OffsetDateTime toExclusive = month.plusMonths(1)
                .atDay(1)
                .atStartOfDay(SERVICE_ZONE)
                .toOffsetDateTime();

        long sampleSize = analysisRepository.countTrendSamples(
                fromAt,
                toExclusive,
                TREND_RISK_LEVELS
        );

        List<AnalysisRepository.CategoryCount> categoryCounts =
                analysisRepository.findTopTrendCategories(
                        fromAt,
                        toExclusive,
                        TREND_RISK_LEVELS,
                        PhishingCategory.OTHER,
                        PageRequest.of(0, 3)
                );
        List<PhishingTypeTrend> topPhishingTypes = java.util.stream.IntStream
                .range(0, categoryCounts.size())
                .mapToObj(index -> new PhishingTypeTrend(
                        index + 1,
                        categoryCounts.get(index).getCategory(),
                        categoryCounts.get(index).getCount()
                ))
                .toList();

        List<AnalysisRepository.KeywordCount> keywordCounts =
                analysisRepository.findTopTrendKeywords(
                        fromAt,
                        toExclusive,
                        TREND_RISK_LEVELS,
                        PageRequest.of(0, 5)
                );
        List<RiskKeywordTrend> topRiskKeywords = java.util.stream.IntStream
                .range(0, keywordCounts.size())
                .mapToObj(index -> {
                    AnalysisRepository.KeywordCount count = keywordCounts.get(index);
                    return new RiskKeywordTrend(
                            index + 1,
                            count.getKeyword(),
                            count.getCount()
                    );
                })
                .toList();

        return new TrendCardResponse(month, sampleSize, topPhishingTypes, topRiskKeywords);
    }

    /** ALL은 시작 시점을 두지 않고, 나머지는 현재 시점에서 지정 일수를 뺀다. */
    private OffsetDateTime resolveFromAt(StatisticsPeriod period, OffsetDateTime now) {
        return switch (period) {
            case LAST_7_DAYS -> now.minusDays(7);
            case LAST_30_DAYS -> now.minusDays(30);
            case LAST_90_DAYS -> now.minusDays(90);
            case ALL -> null;
        };
    }

    /** 기간 유무에 맞는 위험 등급 집계 쿼리를 선택한다. */
    private List<AnalysisRepository.RiskCount> findRiskCounts(Long userId, OffsetDateTime fromAt) {
        return fromAt == null
                ? analysisRepository.countByRiskLevel(userId)
                : analysisRepository.countByRiskLevelSince(userId, fromAt);
    }

    /** 기간 유무에 맞는 피싱 유형 집계 쿼리를 선택한다. */
    private List<AnalysisRepository.CategoryCount> findCategoryCounts(Long userId, OffsetDateTime fromAt) {
        return fromAt == null
                ? analysisRepository.countByCategory(userId)
                : analysisRepository.countByCategorySince(userId, fromAt);
    }

    /** 데이터가 없는 위험 등급도 0건으로 반환할 수 있도록 모든 등급을 초기화한다. */
    private Map<RiskLevel, Long> initializeRiskCounts() {
        Map<RiskLevel, Long> counts = new EnumMap<>(RiskLevel.class);
        Arrays.stream(RiskLevel.values()).forEach(riskLevel -> counts.put(riskLevel, 0L));
        return counts;
    }

    /** 데이터가 없는 피싱 유형도 0건으로 반환할 수 있도록 모든 유형을 초기화한다. */
    private Map<PhishingCategory, Long> initializeCategoryCounts() {
        Map<PhishingCategory, Long> counts = new EnumMap<>(PhishingCategory.class);
        Arrays.stream(PhishingCategory.values()).forEach(category -> counts.put(category, 0L));
        return counts;
    }
}
