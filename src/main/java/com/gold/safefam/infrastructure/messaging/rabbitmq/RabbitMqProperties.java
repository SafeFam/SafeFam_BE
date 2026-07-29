package com.gold.safefam.infrastructure.messaging.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "safefam.messaging.analysis")
public record RabbitMqProperties (
    @DefaultValue("safefam.analysis")
    String exchange,

    @DefaultValue("safefam.analysis.requested.q")
    String requestQueue,

    @DefaultValue("analysis.requested.v1")
    String requestRoutingKey,

    @DefaultValue("safefam.analysis.result.q")
    String resultQueue,

    @DefaultValue("analysis.completed.v1")
    String completedRoutingKey,

    @DefaultValue("analysis.partial.v1")
    String partialRoutingKey,

    @DefaultValue("analysis.failed.v1")
    String failedRoutingKey,

    @DefaultValue("safefam.analysis.result.dlq")
    String resultDlq,

    @DefaultValue("analysis.result.dead.v1")
    String resultDlqRoutingKey,

    @DefaultValue("2")
    int resultMaxDeliveries,

    @DefaultValue("true")
    boolean publisherEnabled,

    boolean consumerEnabled
) {

}
