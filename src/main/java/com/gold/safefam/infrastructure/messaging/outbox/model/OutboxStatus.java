package com.gold.safefam.infrastructure.messaging.outbox.model;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
