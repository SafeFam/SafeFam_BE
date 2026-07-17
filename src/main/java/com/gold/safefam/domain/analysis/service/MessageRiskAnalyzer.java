package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult;

/**
 * 문자 내용과 발신자를 분석하는 핵심 도메인 인터페이스.
 *
 * 현재는 {@link RuleBasedMessageRiskAnalyzer}가 구현체로 등록된다.
 * 다음 API 연결 작업에서 AnalysisService가 이 인터페이스를 주입받아 호출하므로,
 * Controller는 규칙 구현이나 향후 LLM 구현을 직접 알 필요가 없다.
 */
public interface MessageRiskAnalyzer {

    /**
     * 발신자와 문자 원문을 분석해 저장소나 API DTO에 의존하지 않는 도메인 결과를 반환한다.
     * 동일한 입력은 항상 동일한 결과를 반환해야 한다.
     */
    MessageRiskAnalysisResult analyze(String sender, String content);
}
