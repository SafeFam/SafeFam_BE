package com.gold.safefam.domain.analysis.messaging.event;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.service.UrlRiskAnalyzer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/* 분석 완료 및 결과 이벤트 */
public record AnalysisResultEvent(
        String schemaVersion,
        UUID eventId,
        UUID causationId,
        Long analysisId,
        String clientMessageId,
        UUID traceId,
        Instant occurredAt,
        AnalysisEventType eventType,
        Payload payload
) {

    // 최종 스코어링 및 각 트랙별 정밀 분석 데이터
    public record Payload(
            Integer finalScore,
            RiskLevel riskGrade,
            String phishingType,
            RawScores rawScores,
            WeightedContributions weightedContributions,
            TextAnalysis textAnalysis,
            UrlAnalysis urlAnalysis,
            RuleAnalysis ruleAnalysis,
            List<String> failedTracks,
            String failureCode
    ) {
        // 각 분야별 가중치 적용 전 순수 탐지 접수
        public record RawScores(
                Integer text,
                Integer url,
                Integer rules
        ) {
        }

        // 각 분야별 최종 반영 가중 점수
        public record WeightedContributions(
                Integer text,
                Integer url,
                Integer rules
        ) {
        }

        // 텍스트(LLM/NLP) 상세 분석 정보
        public record TextAnalysis(
                String method,
                Integer score,
                String grade,
                String reason,
                List<String> evidence,
                List<String> failedEngines,
                Integer selfModelScore,
                Double selfModelConfidence,
                Boolean geminiCalled,
                String decisionSource,
                String routingReason,
                Boolean fallbackApplied
        ) {
        }

        // URL 검사 상세 분석 정보
        public record UrlAnalysis(
                Boolean hasUrl,
                String originalUrl,
                String tracedUrl,
                Boolean malicious,
                Integer score,
                String engineSource,
                String errorCode
        ){
        }
        
        // 룰 기반 1차 패턴 탐지 정보
        public record RuleAnalysis(
                Integer score,
                List<String> matchedRules,
                Boolean maliciousDomainPattern,
                Boolean institutionContactMismatch,
                List<String> mentionedInstitutions
        ) {
        }
    }
}

