package com.gold.safefam.infrastructure.messaging.outbox.model;

import java.util.UUID;

public record ClaimedOutboxEvent(
        Long id,
        UUID eventId,
        String schemaVersion,
        String encryptedPayload
) {

    public static ClaimedOutboxEvent from(OutboxEvent event) {
        return new ClaimedOutboxEvent(
                event.getId(),
                event.getEventId(),
                event.getSchemaVersion(),
                event.getEncryptedPayload()
        );
    }
}