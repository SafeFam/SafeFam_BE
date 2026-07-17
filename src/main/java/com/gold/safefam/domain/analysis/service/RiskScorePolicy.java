package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 패턴 점수와 URL 점수를 하나의 위험 점수와 등급으로 변환하는 정책 컴포넌트.
 *
 * 탐지 규칙과 분리되어 있어 운영 데이터가 쌓였을 때 가중치와 등급 경계값만
 * 독립적으로 조정할 수 있다.
 */
@Component
public class RiskScorePolicy {

    private static final int PATTERN_WEIGHT_PERCENT = 75;
    private static final int URL_WEIGHT_PERCENT = 25;
    private static final int HIGH_RISK_THRESHOLD = 70;
    private static final int MEDIUM_RISK_THRESHOLD = 35;

    /** 두 계층의 점수와 복합 위험 신호를 반영해 0~100 점수와 등급을 반환한다. */
    public RiskAssessment assess(
            int patternScore,
            int urlScore,
            Set<IndicatorType> detectedTypes
    ) {
        int score = (patternScore * PATTERN_WEIGHT_PERCENT
                + urlScore * URL_WEIGHT_PERCENT) / 100;

        // URL 자체가 매우 위험하면 문자 패턴이 없어도 등급이 지나치게 낮아지지 않도록 보정한다.
        score = Math.max(score, urlScore * 60 / 100);

        // 강한 문자 패턴과 URL 또는 금융 행동이 결합되면 복합 피싱 가능성을 추가 반영한다.
        if (patternScore >= 75 && urlScore > 0) {
            score += 5;
        }
        if (urlScore >= 60 && detectedTypes.contains(IndicatorType.FINANCIAL_ACTION)) {
            score += 5;
        }

        int normalizedScore = clamp(score);
        return new RiskAssessment(normalizedScore, determineLevel(normalizedScore));
    }

    private RiskLevel determineLevel(int riskScore) {
        if (riskScore >= HIGH_RISK_THRESHOLD) {
            return RiskLevel.HIGH;
        }
        if (riskScore >= MEDIUM_RISK_THRESHOLD) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /** 최종 결과 생성 단계에 전달하는 종합 점수와 위험 등급. */
    public record RiskAssessment(int score, RiskLevel level) {
    }
}
