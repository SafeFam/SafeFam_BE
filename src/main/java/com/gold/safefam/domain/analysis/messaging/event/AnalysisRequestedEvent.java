package com.gold.safefam.domain.analysis.messaging.event;


import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AnalysisRequestedEvent(
        String schemaVersion,
        UUID eventId,
        Long analysisId,
        String clientMessageId,
        UUID traceId,
        Instant occurredAt,
        Payload payload
) {
    public record Payload(
            String sender,
            String content,
            LocalDateTime receivedAt,
            String souce
    ) {

    }
}