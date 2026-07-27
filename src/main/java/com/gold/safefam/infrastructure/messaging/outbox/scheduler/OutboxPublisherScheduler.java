package com.gold.safefam.infrastructure.messaging.outbox.scheduler;

import com.gold.safefam.infrastructure.messaging.outbox.service.OutboxPublisherService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "safefam.messaging.analysis",
        name = "publisher-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisherScheduler {

    private final OutboxPublisherService publisherService;

    public OutboxPublisherScheduler(
            OutboxPublisherService publisherService
    ) {
        this.publisherService = publisherService;
    }

    @Scheduled(
            fixedDelayString =
                    "${safefam.messaging.analysis.publisher-delay:1000}"
    )
    public void publishPendingEvents() {
        publisherService.publishPendingEvents();
    }
}