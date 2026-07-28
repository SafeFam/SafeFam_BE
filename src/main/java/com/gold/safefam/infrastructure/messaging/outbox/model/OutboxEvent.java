package com.gold.safefam.infrastructure.messaging.outbox.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Entity
@Table(name = "outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion;

    @Column(name = "encrypted_payload", nullable = false, columnDefinition = "TEXT")
    private String encryptedPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    public static OutboxEvent pending(
            UUID eventId,
            String aggregateType,
            Long aggregateId,
            String eventType,
            String schemaVersion,
            String encryptedPayload
    ) {
        OutboxEvent event = new OutboxEvent();

        event.eventId = eventId;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.schemaVersion = schemaVersion;
        event.encryptedPayload = encryptedPayload;
        event.status = OutboxStatus.PENDING;
        event.attempts = 0;
        event.nextAttemptAt = OffsetDateTime.now(ZoneOffset.UTC);
        event.createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        return event;
    }

    public void markProcessing(OffsetDateTime recoveryAt) {
        this.status = OutboxStatus.PROCESSING;
        this.nextAttemptAt = recoveryAt;
        this.lastError = null;
    }

    public void markPublished(OffsetDateTime publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void markRetry(
            OffsetDateTime nextAttemptAt,
            String errorMessage
    ) {
        this.status = OutboxStatus.PENDING;
        this.attempts++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = limitErrorMessage(errorMessage);
    }

    public void markFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.attempts++;
        this.nextAttemptAt = null;
        this.lastError = limitErrorMessage(errorMessage);
    }

    private String limitErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Unknown publishing error";
        }

        return errorMessage.length() <= 500
                ? errorMessage
                : errorMessage.substring(0, 500);
    }
}
