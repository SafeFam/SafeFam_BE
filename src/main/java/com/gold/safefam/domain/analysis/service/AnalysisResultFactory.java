package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult.RecommendedAction;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult.RiskIndicator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 분석 컴포넌트들의 중간 결과를 최종 도메인 결과로 조립하는 컴포넌트.
 *
 * 점수를 다시 계산하지 않고 위험 등급과 피싱 유형에 맞는 설명·권장 행동만 생성한다.
 * 따라서 사용자 문구나 상담 번호 변경이 탐지 규칙에 영향을 주지 않는다.
 */
@Component
public class AnalysisResultFactory {

    /** 패턴·URL·점수·유형 결과를 하나의 불변 MessageRiskAnalysisResult로 조립한다. */
    public MessageRiskAnalysisResult create(
            MessagePatternDetector.PatternDetection pattern,
            UrlRiskAnalyzer.UrlAnalysis url,
            RiskScorePolicy.RiskAssessment risk,
            PhishingCategory category,
            List<RiskIndicator> indicators
    ) {
        return new MessageRiskAnalysisResult(
                risk.score(),
                risk.level(),
                category,
                pattern.score(),
                url.score(),
                indicators,
                url.urls(),
                buildExplanation(risk.level(), indicators),
                recommendedActionsFor(risk.level(), category)
        );
    }

    /** 탐지 근거 enum을 사용자가 이해할 수 있는 한 줄 요약으로 만든다. */
    private String buildExplanation(RiskLevel riskLevel, List<RiskIndicator> indicators) {
        if (indicators.isEmpty()) {
            return "현재 규칙에서 뚜렷한 금융 사기 위험 신호가 발견되지 않았습니다.";
        }

        String detected = indicators.stream()
                .map(indicator -> indicator.type().name())
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return "위험 단계 " + riskLevel.name() + ": " + detected + " 신호가 탐지되었습니다.";
    }

    /** 위험 단계에는 공통 안전 행동을, 피싱 유형에는 관련 신고·상담 번호를 연결한다. */
    public List<RecommendedAction> recommendedActionsFor(
            RiskLevel riskLevel,
            PhishingCategory category
    ) {
        List<RecommendedAction> actions = new ArrayList<>();
        if (riskLevel == RiskLevel.LOW) {
            actions.add(new RecommendedAction(
                    "CHECK_SENDER",
                    "발신자와 안내 내용을 공식 앱에서 한 번 더 확인하세요.",
                    null,
                    null
            ));
            return actions;
        }

        actions.add(new RecommendedAction(
                "DO_NOT_CLICK",
                "문자에 포함된 링크를 누르거나 개인정보를 입력하지 마세요.",
                null,
                null
        ));
        actions.add(new RecommendedAction(
                "VERIFY_OFFICIAL_CHANNEL",
                "문자에 적힌 연락처가 아닌 기관 공식 대표번호로 사실을 확인하세요.",
                null,
                null
        ));

        if (category == PhishingCategory.FINANCIAL_INSTITUTION
                || category == PhishingCategory.LOAN) {
            actions.add(new RecommendedAction(
                    "CALL_1332",
                    "금융감독원 1332에 상담하세요.",
                    "1332",
                    "https://www.fss.or.kr"
            ));
        } else if (category == PhishingCategory.GOVERNMENT_AGENCY) {
            actions.add(new RecommendedAction(
                    "CALL_112",
                    "수사기관 사칭이 의심되면 경찰 112에 확인하세요.",
                    "112",
                    null
            ));
        }
        return actions;
    }
}
