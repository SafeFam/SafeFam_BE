package com.gold.safefam.infrastructure.messaging.outbox.repository;

import com.gold.safefam.infrastructure.messaging.outbox.model.OutboxStatus;
import com.gold.safefam.infrastructure.messaging.outbox.model.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    boolean existsByEventId(UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT event
            FROM OutboxEvent event
            WHERE (
                event.status = :pendingStatus
                AND event.nextAttemptAt <= :now
            )
            OR (
                event.status = :processingStatus
                AND event.nextAttemptAt <= :now
            )
            ORDER BY event.createdAt ASC
            """)
    List<OutboxEvent> findClaimable(
            @Param("pendingStatus") OutboxStatus pendingStatus,
            @Param("processingStatus") OutboxStatus processingStatus,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );
}