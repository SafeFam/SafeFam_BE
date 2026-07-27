package com.gold.safefam.instrastructure.messaging.rabbimq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safefam.messaging.analysis")
public record RabbitMqProperties (
    String excahnge,
    String requestQueue,
    String requestRoutingKey,
    boolean publisherEnabled
) {

}
