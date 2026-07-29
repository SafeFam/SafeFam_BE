package com.gold.safefam.infrastructure.messaging.rabbitmq.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();
    private final RabbitMqProperties properties =
            new RabbitMqProperties(
                    "safefam.analysis",
                    "request.q",
                    "analysis.requested.v1",
                    "result.q",
                    "analysis.completed.v1",
                    "analysis.partial.v1",
                    "analysis.failed.v1",
                    "result.dlq",
                    "analysis.result.dead.v1",
                    3,
                    10,
                    true,
                    true
            );

    @Test
    void createsDurableQuorumResultQueueWithDeadLetterPolicy() {
        Queue queue = config.analysisResultQueue(properties);

        assertTrue(queue.isDurable());
        assertEquals("quorum", queue.getArguments().get("x-queue-type"));
        assertEquals(3, queue.getArguments().get("x-delivery-limit"));
        assertEquals(
                "safefam.analysis",
                queue.getArguments().get("x-dead-letter-exchange")
        );
        assertEquals(
                "analysis.result.dead.v1",
                queue.getArguments().get("x-dead-letter-routing-key")
        );
    }

    @Test
    void bindsAllResultRoutingKeysToSameQueue() {
        Queue queue = config.analysisResultQueue(properties);
        TopicExchange exchange = config.analysisExchange(properties);

        Binding completed =
                config.completedResultBinding(queue, exchange, properties);
        Binding partial =
                config.partialResultBinding(queue, exchange, properties);
        Binding failed =
                config.failedResultBinding(queue, exchange, properties);

        assertEquals("analysis.completed.v1", completed.getRoutingKey());
        assertEquals("analysis.partial.v1", partial.getRoutingKey());
        assertEquals("analysis.failed.v1", failed.getRoutingKey());
        assertEquals("result.q", completed.getDestination());
        assertEquals("result.q", partial.getDestination());
        assertEquals("result.q", failed.getDestination());
    }
}
