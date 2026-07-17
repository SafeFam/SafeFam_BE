package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult.RiskIndicator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 규칙 기반 문자 분석의 실행 순서만 조율하는 진입점.
 *
 * 실제 탐지 규칙, URL 검사, 점수 정책, 유형 분류, 결과 문구 생성은 각각의
 * 전용 컴포넌트에 위임한다. 이후 AnalysisService는 이 클래스가 아니라
 * MessageRiskAnalyzer 인터페이스만 주입받아 분석 기능을 사용한다.
 */
@Component
public class RuleBasedMessageRiskAnalyzer implements MessageRiskAnalyzer {

    private final MessagePatternDetector patternDetector;
    private final UrlRiskAnalyzer urlRiskAnalyzer;
    private final RiskScorePolicy riskScorePolicy;
    private final PhishingCategoryClassifier categoryClassifier;
    private final AnalysisResultFactory resultFactory;

    public RuleBasedMessageRiskAnalyzer(
            MessagePatternDetector patternDetector,
            UrlRiskAnalyzer urlRiskAnalyzer,
            RiskScorePolicy riskScorePolicy,
            PhishingCategoryClassifier categoryClassifier,
            AnalysisResultFactory resultFactory
    ) {
        this.patternDetector = patternDetector;
        this.urlRiskAnalyzer = urlRiskAnalyzer;
        this.riskScorePolicy = riskScorePolicy;
        this.categoryClassifier = categoryClassifier;
        this.resultFactory = resultFactory;
    }

    /**
     * 입력 검증부터 최종 결과 생성까지 각 컴포넌트를 정해진 순서로 호출한다.
     * 이 메서드에는 개별 키워드나 점수 상수를 두지 않아 분석 정책 변경의 영향을 줄인다.
     */
    @Override
    public MessageRiskAnalysisResult analyze(String sender, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }

        String normalizedSender = sender == null ? "" : sender.trim();
        String normalizedContent = content.trim();
        String analysisText = normalizedSender + " " + normalizedContent;

        // 1. 문자 표현에서 사칭·금융행동·개인정보·긴급성 위험 신호를 찾는다.
        MessagePatternDetector.PatternDetection pattern = patternDetector.detect(analysisText);

        // 2. 문자 본문에서 URL을 추출하고 외부 조회 없이 형태 기반 위험 신호를 찾는다.
        UrlRiskAnalyzer.UrlAnalysis url = urlRiskAnalyzer.analyze(normalizedContent);

        // 3. 두 분석기의 근거 유형을 합쳐 복합 위험 점수 계산에 사용한다.
        Set<IndicatorType> detectedTypes = new LinkedHashSet<>(pattern.detectedTypes());
        detectedTypes.addAll(url.detectedTypes());

        // 4. 독립된 점수 정책이 종합 점수와 LOW/MEDIUM/HIGH 등급을 결정한다.
        RiskScorePolicy.RiskAssessment risk = riskScorePolicy.assess(
                pattern.score(),
                url.score(),
                detectedTypes
        );

        // 5. 점수와 별개로 가장 많이 일치한 주제를 이용해 피싱 유형을 결정한다.
        PhishingCategory category = categoryClassifier.classify(analysisText);

        // 6. 모든 탐지 근거를 순서대로 합쳐 외부 계층이 사용할 불변 결과를 만든다.
        List<RiskIndicator> indicators = new ArrayList<>(pattern.indicators());
        indicators.addAll(url.indicators());
        return resultFactory.create(pattern, url, risk, category, indicators);
    }
}
