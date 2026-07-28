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

    @DefaultValue("true")
    boolean publisherEnabled
) {

}
