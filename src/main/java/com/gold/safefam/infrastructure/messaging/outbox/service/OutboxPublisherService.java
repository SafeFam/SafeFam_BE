package com.gold.safefam.infrastructure.messaging.outbox.service;

import com.gold.safefam.infrastructure.messaging.outbox.model.ClaimedOutboxEvent;
import com.gold.safefam.infrastructure.messaging.rabbitmq.AnalysisRequestEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OutboxPublisherService {

    private static final int BATCH_SIZE = 20;
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventStateService stateService;
    private final AnalysisRequestEventPublisher eventPublisher;

    public OutboxPublisherService(
            OutboxEventStateService stateService,
            AnalysisRequestEventPublisher eventPublisher
    ) {
        this.stateService = stateService;
        this.eventPublisher = eventPublisher;
    }

    // 주기적인 스케줄러나 릴레이가 호출하는 메인 실행 메서드
    public void publishPendingEvents() {
        List<ClaimedOutboxEvent> events =
                stateService.claimBatch(BATCH_SIZE);

        for (ClaimedOutboxEvent event : events) {
            publishOne(event);
        }
    }

    // 단건 이벤트 처리 및 예외 격리
    private void publishOne(ClaimedOutboxEvent event) {
        try {
            eventPublisher.publish(event);
            stateService.markPublished(event.id());

            log.info(
                    "Published analysis request event. eventId={}, outboxId={}",
                    event.eventId(),
                    event.id()
            );
        } catch (Exception exception) {
            stateService.markFailed(
                    event.id(),
                    safeErrorMessage(exception),
                    MAX_ATTEMPTS
            );

            log.warn(
                    "Failed to publish analysis request event. "
                            + "eventId={}, outboxId={}, reason={}",
                    event.eventId(),
                    event.id(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    // DB에 안전하게 저장할 에러 메시지 포맷팅 메서드
    private String safeErrorMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return exception.getClass().getSimpleName()
                + ": "
                + message;
    }
}