package com.gold.safefam.support;

import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisEventType;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AnalysisResultEventFixture {

    private AnalysisResultEventFixture() {
    }

    public static AnalysisResultEvent completed() {
        return event(
                AnalysisEventType.ANALYSIS_COMPLETED,
                successfulPayload(List.of())
        );
    }

    public static AnalysisResultEvent partial() {
        return event(
                AnalysisEventType.ANALYSIS_PARTIAL,
                successfulPayload(List.of("URL"))
        );
    }

    public static AnalysisResultEvent failed() {
        return event(
                AnalysisEventType.ANALYSIS_FAILED,
                new AnalysisResultEvent.Payload(
                        null, null, null, null, null,
                        null, null, null,
                        null,
                        List.of("PIPELINE"), "PIPELINE_ERROR"
                )
        );
    }

    public static AnalysisResultEvent institutionDomainMismatch() {
        AnalysisResultEvent.Payload base = successfulPayload(List.of());
        return event(
                AnalysisEventType.ANALYSIS_COMPLETED,
                new AnalysisResultEvent.Payload(
                        base.finalScore(),
                        base.riskGrade(),
                        base.phishingType(),
                        base.rawScores(),
                        base.weightedContributions(),
                        base.textAnalysis(),
                        base.urlAnalysis(),
                        new AnalysisResultEvent.Payload.RuleAnalysis(
                                80,
                                List.of("기관명-공식 도메인 불일치"),
                                false,
                                new AnalysisResultEvent.Payload.InstitutionMatch(
                                        true,
                                        true,
                                        "KB국민은행",
                                        List.of("kbstar.com"),
                                        "fake-kb.example"
                                )
                        ),
                        base.evidenceCards(),
                        base.failedTracks(),
                        base.failureCode()
                )
        );
    }

    public static AnalysisResultEvent institutionComparisonSkipped() {
        AnalysisResultEvent.Payload base = successfulPayload(List.of());
        return event(
                AnalysisEventType.ANALYSIS_COMPLETED,
                new AnalysisResultEvent.Payload(
                        base.finalScore(),
                        base.riskGrade(),
                        base.phishingType(),
                        base.rawScores(),
                        base.weightedContributions(),
                        base.textAnalysis(),
                        base.urlAnalysis(),
                        new AnalysisResultEvent.Payload.RuleAnalysis(
                                15,
                                List.of("금융기관/공공기관 명칭 언급"),
                                false,
                                new AnalysisResultEvent.Payload.InstitutionMatch(
                                        false,
                                        false,
                                        null,
                                        List.of(),
                                        null
                                )
                        ),
                        base.evidenceCards(),
                        base.failedTracks(),
                        base.failureCode()
                )
        );
    }

    public static AnalysisResultEvent event(
            AnalysisEventType eventType,
            AnalysisResultEvent.Payload payload
    ) {
        return new AnalysisResultEvent(
                "1.0",
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                "message-1",
                UUID.randomUUID(),
                Instant.parse("2026-07-29T00:00:00Z"),
                eventType,
                payload
        );
    }

    public static AnalysisResultEvent.Payload successfulPayload(
            List<String> failedTracks
    ) {
        return new AnalysisResultEvent.Payload(
                82,
                RiskLevel.HIGH,
                "OTHER",
                new AnalysisResultEvent.Payload.RawScores(90, 70, 80),
                new AnalysisResultEvent.Payload.WeightedContributions(
                        50, 20, 12
                ),
                new AnalysisResultEvent.Payload.TextAnalysis(
                        "STACKING_LLM",
                        90,
                        "HIGH",
                        "Suspicious payment request",
                        List.of("Urgent transfer request"),
                        List.of(),
                        70,
                        0.72,
                        true,
                        "AWS_BEDROCK",
                        "anthropic.claude-haiku-4-5-20251001-v1:0",
                        true,
                        "LLM",
                        "UNCERTAIN_SELF_MODEL_PREDICTION",
                        false
                ),
                new AnalysisResultEvent.Payload.UrlAnalysis(
                        true,
                        "https://short.example/a",
                        "https://malicious.example/login",
                        true,
                        70,
                        "GSB",
                        null
                ),
                new AnalysisResultEvent.Payload.RuleAnalysis(
                        80,
                        List.of("URGENT_TRANSFER"),
                        true,
                        null
                ),
                List.of(
                        new AnalysisResultEvent.Payload.EvidenceCard(
                                "DANGEROUS_URL",
                                "위험 URL",
                                "문자에 포함된 링크가 위험한 것으로 확인됐습니다."
                        ),
                        new AnalysisResultEvent.Payload.EvidenceCard(
                                "URGENCY_PRESSURE",
                                "행동 압박",
                                "빠른 판단을 재촉하는 표현이 있습니다."
                        )
                ),
                failedTracks,
                null
        );
    }
}
