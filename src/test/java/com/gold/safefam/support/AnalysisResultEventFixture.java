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
                        List.of("PIPELINE"), "PIPELINE_ERROR"
                )
        );
    }

    public static AnalysisResultEvent institutionContactMismatch() {
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
                                List.of("공식 연락처 교차검증 불일치"),
                                false,
                                true,
                                List.of("KB국민은행")
                        ),
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
                        "NAIVE_BAYES_GEMINI",
                        90,
                        "HIGH",
                        "Suspicious payment request",
                        List.of("Urgent transfer request"),
                        List.of()
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
                        false,
                        List.of()
                ),
                failedTracks,
                null
        );
    }
}
