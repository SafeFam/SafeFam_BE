package com.gold.safefam.infrastructure.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    boolean existsByEventId(UUID eventId);
}