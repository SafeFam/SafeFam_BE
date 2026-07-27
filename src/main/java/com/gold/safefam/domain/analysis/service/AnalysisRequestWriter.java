package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.dto.AnalysisAcceptedResponse;
import com.gold.safefam.domain.analysis.dto.AnalysisRequest;
import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisRequestedEvent;
import com.gold.safefam.domain.analysis.privacy.MessageContentProtector.ProtectedContent;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.infrastructure.messaging.outbox.model.OutboxEvent;
import com.gold.safefam.infrastructure.messaging.outbox.repository.OutboxEventRepository;
import com.gold.safefam.infrastructure.messaging.outbox.service.OutboxPayloadCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class AnalysisRequestWriter {

    private static final String SCHEMA_VERSION = "1.0";
    private static final String AGGREGATE_TYPE = "ANALYSIS";
    private static final String EVENT_TYPE = "ANALYSIS_REQUESTED";

    private final AnalysisRepository analysisRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPayloadCipher payloadCipher;
    private final ObjectMapper objectMapper;

    public AnalysisRequestWriter(
            AnalysisRepository analysisRepository,
            OutboxEventRepository outboxEventRepository,
            OutboxPayloadCipher payloadCipher,
            ObjectMapper objectMapper
    ) {
        this.analysisRepository = analysisRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.payloadCipher = payloadCipher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisAcceptedResponse create(
            Long userId,
            String clientMessageId,
            AnalysisRequest request,
            ProtectedContent protectedContent
    ) {
        // 분석 요청 엔티티 생성
        Analysis analysis = Analysis.pending(
                userId,
                clientMessageId,
                request.sender(),
                protectedContent.hash(),
                protectedContent.preview(),
                request.source(),
                request.receivedAt()
        );

        Analysis saved = analysisRepository.saveAndFlush(analysis);

        // 메시지 발행을 위한 고유 식별자 및 이벤트 객체 생성
        UUID eventId = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        AnalysisRequestedEvent requestedEvent =
                new AnalysisRequestedEvent(
                        SCHEMA_VERSION,
                        eventId,
                        saved.getId(),
                        clientMessageId,
                        traceId,
                        occurredAt,
                        new AnalysisRequestedEvent.Payload(
                                request.sender(),
                                request.content(),
                                request.receivedAt(),
                                request.source()
                        )
                );

        String payloadJson =
                objectMapper.writeValueAsString(requestedEvent);

        String encryptedPayload =
                payloadCipher.encrypt(payloadJson, eventId);

        // Transactional Outbox 패턴
        OutboxEvent outboxEvent = OutboxEvent.pending(
                eventId,
                AGGREGATE_TYPE,
                saved.getId(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                encryptedPayload
        );

        outboxEventRepository.save(outboxEvent);

        return new AnalysisAcceptedResponse(
                saved.getId(),
                saved.getStatus()
        );
    }
}