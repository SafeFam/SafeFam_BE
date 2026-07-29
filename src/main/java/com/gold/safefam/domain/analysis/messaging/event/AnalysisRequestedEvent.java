package com.gold.safefam.domain.analysis.messaging.event;

import com.gold.safefam.domain.analysis.enums.AnalysisSource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/* 분석 요청 이벤트 */
public record AnalysisRequestedEvent(
        String schemaVersion,
        UUID eventId,
        Long analysisId,
        String clientMessageId,
        UUID traceId,
        Instant occurredAt,
        Payload payload
) {
    // 분석에 필요한 실제 데이터 객체
    public record Payload(
            String sender,
            String content,
            OffsetDateTime receivedAt,
            AnalysisSource source
    ) {
    }
}