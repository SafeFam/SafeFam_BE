package com.gold.safefam.domain.analysis.model;

import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;

import java.util.List;

/**
 * 외부 API나 저장소에 의존하지 않는 문자 위험 분석 결과.
 * 이후 AnalysisService에서 API 응답과 영속 엔티티로 변환해 사용한다.
 *
 * 처리 흐름:
 * RuleBasedMessageRiskAnalyzer가 이 결과를 생성하고,
 * AnalysisService가 필요한 값만 DB에 저장한 뒤 AnalysisResponse로 변환한다.
 */
public record MessageRiskAnalysisResult(
        int riskScore,
        RiskLevel riskLevel,
        PhishingCategory category,
        int patternScore,
        int urlScore,
        List<RiskIndicator> indicators,
        List<UrlRisk> urls,
        String explanation,
        List<RecommendedAction> recommendedActions
) {

    public MessageRiskAnalysisResult {
        // 호출자가 결과 목록을 변경해 분석 결과가 달라지는 일을 막기 위해 불변 목록으로 복사한다.
        indicators = List.copyOf(indicators);
        urls = List.copyOf(urls);
        recommendedActions = List.copyOf(recommendedActions);
    }

    /** 점수 산정에 반영된 사칭·금융행동·개인정보 요구 등의 근거 한 건. */
    public record RiskIndicator(
            IndicatorType type,
            String description
    ) {
    }

    /** 외부 평판 조회 없이 URL 형태만으로 확인한 휴리스틱 위험 결과. */
    public record UrlRisk(
            String originalUrl,
            boolean shortened,
            boolean suspicious,
            List<String> reasons
    ) {
        public UrlRisk {
            // URL 판정 사유도 분석 결과와 함께 불변으로 유지한다.
            reasons = List.copyOf(reasons);
        }
    }

    /** 위험 단계와 피싱 유형에 따라 사용자에게 제시할 즉시 대응 방법. */
    public record RecommendedAction(
            String type,
            String label,
            String phoneNumber,
            String url
    ) {
    }
}
