package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult.RiskIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 발신자와 문자 본문에서 언어적 위험 패턴을 찾는 컴포넌트.
 *
 * 이 클래스는 URL이나 최종 위험 등급을 판단하지 않는다. 각 위험 유형의 기본 점수와
 * 복합 신호 보너스만 계산해 RuleBasedMessageRiskAnalyzer에 반환한다.
 */
@Component
public class MessagePatternDetector {

    private static final List<IndicatorRule> RULES = List.of(
            new IndicatorRule(
                    IndicatorType.IMPERSONATION,
                    pattern("은행|금융감독원|금감원|검찰|경찰|국세청|건강보험|공단|우체국|카드사|수사관|검사입니다|고객센터"),
                    20,
                    "기관 또는 담당자를 사칭하는 표현이 포함되어 있습니다."
            ),
            new IndicatorRule(
                    IndicatorType.FINANCIAL_ACTION,
                    pattern("송금|입금|이체|납부|상환|대출|계좌|현금|수수료|보증금|안전계좌|예치금"),
                    25,
                    "송금·입금·대출 등 금융 행동을 유도하는 표현이 포함되어 있습니다."
            ),
            new IndicatorRule(
                    IndicatorType.SENSITIVE_INFORMATION,
                    pattern("주민등록번호|주민번호|비밀번호|인증번호|보안카드|OTP|신분증|계좌번호|카드번호|개인정보"),
                    25,
                    "인증정보 또는 개인정보 제공을 요구하는 표현이 포함되어 있습니다."
            ),
            new IndicatorRule(
                    IndicatorType.URGENCY,
                    pattern("즉시|긴급|오늘까지|지금 바로|미납|정지|차단|압류|체포|기한|마감|불이익|곧 종료"),
                    15,
                    "즉시 행동하지 않으면 불이익이 생긴다고 압박하는 표현이 포함되어 있습니다."
            )
    );

    /** 위험 표현과 복합 조합을 찾아 0~100 패턴 점수와 근거 목록을 반환한다. */
    public PatternDetection detect(String analysisText) {
        Set<IndicatorType> detectedTypes = new LinkedHashSet<>();
        List<RiskIndicator> indicators = new java.util.ArrayList<>();
        int score = 0;

        // 같은 위험 유형의 단어가 여러 번 나와도 기본 점수와 근거는 한 번만 추가한다.
        for (IndicatorRule rule : RULES) {
            if (rule.pattern().matcher(analysisText).find()) {
                score += rule.weight();
                detectedTypes.add(rule.type());
                indicators.add(new RiskIndicator(rule.type(), rule.description()));
            }
        }

        // 실제 피싱에서 자주 함께 등장하는 위험 신호 조합에는 추가 점수를 준다.
        if (containsAll(detectedTypes, IndicatorType.IMPERSONATION, IndicatorType.FINANCIAL_ACTION)) {
            score += 10;
        }
        if (containsAll(detectedTypes, IndicatorType.FINANCIAL_ACTION, IndicatorType.SENSITIVE_INFORMATION)) {
            score += 10;
        }
        if (containsAll(detectedTypes, IndicatorType.FINANCIAL_ACTION, IndicatorType.URGENCY)) {
            score += 10;
        }

        return new PatternDetection(Math.min(100, score), indicators, detectedTypes);
    }

    private boolean containsAll(Set<IndicatorType> detectedTypes, IndicatorType first, IndicatorType second) {
        return detectedTypes.contains(first) && detectedTypes.contains(second);
    }

    private static Pattern pattern(String expression) {
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** 패턴 탐지기가 다음 점수 단계에 전달하는 중간 결과. */
    public record PatternDetection(
            int score,
            List<RiskIndicator> indicators,
            Set<IndicatorType> detectedTypes
    ) {
        public PatternDetection {
            indicators = List.copyOf(indicators);
            detectedTypes = Set.copyOf(detectedTypes);
        }
    }

    /** 하나의 위험 유형을 탐지하는 정규식과 점수, 사용자 설명을 묶은 내부 규칙. */
    private record IndicatorRule(
            IndicatorType type,
            Pattern pattern,
            int weight,
            String description
    ) {
    }
}
