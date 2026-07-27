package com.gold.safefam.infrastructure.messaging.outbox.service;

import com.gold.safefam.infrastructure.messaging.outbox.model.OutboxStatus;
import com.gold.safefam.infrastructure.messaging.outbox.model.ClaimedOutboxEvent;
import com.gold.safefam.infrastructure.messaging.outbox.model.OutboxEvent;
import com.gold.safefam.infrastructure.messaging.outbox.repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class OutboxEventStateService {

    private static final Duration PROCESSING_TIMEOUT =
            Duration.ofMinutes(1);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventStateService(
            OutboxEventRepository outboxEventRepository
    ) {
        this.outboxEventRepository = outboxEventRepository;
    }

    // 배치 선점: 처리할 Outbox 이벤트를 배치 단위로 가져온 뒤 'PROCESSING' 상태로 전환
    @Transactional
    public List<ClaimedOutboxEvent> claimBatch(int batchSize) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<OutboxEvent> events =
                outboxEventRepository.findClaimable(
                        OutboxStatus.PENDING,
                        OutboxStatus.PROCESSING,
                        now,
                        PageRequest.of(0, batchSize)
                );

        OffsetDateTime recoveryAt =
                now.plus(PROCESSING_TIMEOUT);

        events.forEach(event ->
                event.markProcessing(recoveryAt)
        );

        return events.stream()
                .map(ClaimedOutboxEvent::from)
                .toList();
    }

    // 발행 성공 처리: RabbitMQ 전송 완료 후 이벤트를 'PUBLISHED' 상태로 변경
    @Transactional
    public void markPublished(Long outboxId) {
        OutboxEvent event = getOutboxEvent(outboxId);

        event.markPublished(
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    // 발행 실패 및 재시도 처리: 실패 횟수에 따라 'RETRY(재시도)' 대기 상태로 변경하거나 'FAILED(최종 실패)' 처리
    @Transactional
    public void markFailed(
            Long outboxId,
            String errorMessage,
            int maxAttempts
    ) {
        OutboxEvent event = getOutboxEvent(outboxId);

        int nextAttempt = event.getAttempts() + 1;

        if (nextAttempt >= maxAttempts) {
            event.markFailed(errorMessage);
            return;
        }

        OffsetDateTime nextAttemptAt =
                OffsetDateTime.now(ZoneOffset.UTC)
                        .plusSeconds(retryDelaySeconds(nextAttempt));

        event.markRetry(nextAttemptAt, errorMessage);
    }

    private OutboxEvent getOutboxEvent(Long outboxId) {
        return outboxEventRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event not found: " + outboxId
                ));
    }

    // 지수 백오프 딜레이 전략: 재시도 횟수별 대기 시간(초) 설정
    private long retryDelaySeconds(int attempt) {
        return switch (attempt) {
            case 1 -> 5;
            case 2 -> 30;
            case 3 -> 120;
            default -> 600;
        };
    }
}