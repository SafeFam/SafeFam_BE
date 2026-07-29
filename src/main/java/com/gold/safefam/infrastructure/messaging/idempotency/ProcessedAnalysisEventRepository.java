package com.gold.safefam.infrastructure.messaging.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ProcessedAnalysisEventRepository
        extends JpaRepository<ProcessedAnalysisEvent, UUID> {

    @Modifying
    @Query(
            value = """
            INSERT INTO processed_analysis_events(
                event_id,
                analysis_id,
                event_type,
                processed_at
            )
            VALUES (
                :eventId,
                :analysisId,
                :eventType,
                :processedAt
            )
            ON CONFLICT (event_id) DO NOTHING
            """,
            nativeQuery = true
    )
    int insertIfAbsent(
            UUID eventId,
            Long analysisId,
            String eventType,
            OffsetDateTime processedAt
    );
}
