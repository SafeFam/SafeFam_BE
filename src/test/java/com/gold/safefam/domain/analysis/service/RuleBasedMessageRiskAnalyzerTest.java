package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Context나 DB를 띄우지 않고 핵심 분석 규칙만 빠르게 검증한다.
 * 각 테스트는 점수의 정확한 숫자보다 위험 단계와 필수 탐지 근거를 고정해
 * 향후 가중치를 조정할 수 있으면서도 핵심 동작이 깨지지 않게 한다.
 */
class RuleBasedMessageRiskAnalyzerTest {

    // Spring을 띄우지 않는 순수 단위 테스트이므로 운영과 동일한 컴포넌트를 직접 조립한다.
    private final RuleBasedMessageRiskAnalyzer analyzer = new RuleBasedMessageRiskAnalyzer(
            new MessagePatternDetector(),
            new UrlRiskAnalyzer(),
            new RiskScorePolicy(),
            new PhishingCategoryClassifier(),
            new AnalysisResultFactory()
    );

    @Test
    void financialImpersonationWithSensitiveRequestAndShortUrlIsHighRisk() {
        // 사칭·금융행동·개인정보·긴급성·위험 URL이 결합된 대표적인 고위험 문자다.
        MessageRiskAnalysisResult result = analyzer.analyze(
                "국민은행",
                "[긴급] 계좌가 정지됩니다. 즉시 https://bit.ly/secure-login 에서 "
                        + "인증번호를 입력하고 안전계좌로 이체하세요."
        );

        assertEquals(RiskLevel.HIGH, result.riskLevel());
        assertEquals(PhishingCategory.FINANCIAL_INSTITUTION, result.category());
        assertTrue(result.riskScore() >= 70);
        assertTrue(hasIndicator(result, IndicatorType.IMPERSONATION));
        assertTrue(hasIndicator(result, IndicatorType.FINANCIAL_ACTION));
        assertTrue(hasIndicator(result, IndicatorType.SENSITIVE_INFORMATION));
        assertTrue(hasIndicator(result, IndicatorType.URGENCY));
        assertTrue(hasIndicator(result, IndicatorType.SHORTENED_URL));
        assertTrue(hasIndicator(result, IndicatorType.MALICIOUS_URL));
        assertTrue(result.recommendedActions().stream()
                .anyMatch(action -> "CALL_1332".equals(action.type())));
    }

    @Test
    void governmentImpersonationProducesGovernmentCategoryAndPoliceAction() {
        // 수사기관 사칭은 정부기관 유형과 112 확인 행동으로 이어져야 한다.
        MessageRiskAnalysisResult result = analyzer.analyze(
                "서울중앙지검",
                "검찰 수사관입니다. 지금 바로 주민번호를 제출하지 않으면 체포될 수 있습니다."
        );

        assertEquals(PhishingCategory.GOVERNMENT_AGENCY, result.category());
        assertEquals(RiskLevel.MEDIUM, result.riskLevel());
        assertTrue(hasIndicator(result, IndicatorType.IMPERSONATION));
        assertTrue(hasIndicator(result, IndicatorType.SENSITIVE_INFORMATION));
        assertTrue(hasIndicator(result, IndicatorType.URGENCY));
        assertTrue(result.recommendedActions().stream()
                .anyMatch(action -> "CALL_112".equals(action.type())));
    }

    @Test
    void suspiciousIpLoginUrlIsAtLeastMediumRiskWithoutExternalReputationLookup() {
        // 평판 API 없이도 IP 주소와 로그인 경로 조합은 최소 MEDIUM으로 판정한다.
        MessageRiskAnalysisResult result = analyzer.analyze(
                "WEB발신",
                "본인 확인: http://192.168.0.10/login"
        );

        assertEquals(RiskLevel.MEDIUM, result.riskLevel());
        assertEquals(1, result.urls().size());
        assertTrue(result.urls().get(0).suspicious());
        assertFalse(result.urls().get(0).shortened());
        assertTrue(hasIndicator(result, IndicatorType.MALICIOUS_URL));
    }

    @Test
    void ordinaryAppointmentMessageIsLowRisk() {
        // 위험 키워드가 없는 일상 안내 문자는 오탐하지 않아야 한다.
        MessageRiskAnalysisResult result = analyzer.analyze(
                "한성치과",
                "내일 오후 3시 진료 예약입니다. 일정 변경이 필요하면 대표번호로 연락해 주세요."
        );

        assertEquals(RiskLevel.LOW, result.riskLevel());
        assertEquals(PhishingCategory.OTHER, result.category());
        assertEquals(0, result.riskScore());
        assertTrue(result.indicators().isEmpty());
        assertTrue(result.urls().isEmpty());
    }

    @Test
    void sameInputAlwaysProducesSameResult() {
        // 규칙 기반 분석은 같은 입력에 항상 같은 결과를 반환해야 캐시와 중복 방지에 사용할 수 있다.
        MessageRiskAnalysisResult first = analyzer.analyze(
                "택배",
                "배송 주소를 확인하세요. https://tinyurl.com/verify"
        );
        MessageRiskAnalysisResult second = analyzer.analyze(
                "택배",
                "배송 주소를 확인하세요. https://tinyurl.com/verify"
        );

        assertEquals(first, second);
        assertEquals(PhishingCategory.DELIVERY, first.category());
    }

    @Test
    void blankContentIsRejected() {
        // Controller 밖에서 직접 호출하더라도 빈 입력을 허용하지 않는다.
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze("sender", " "));
    }

    private boolean hasIndicator(
            MessageRiskAnalysisResult result,
            IndicatorType type
    ) {
        return result.indicators().stream().anyMatch(indicator -> indicator.type() == type);
    }
}
