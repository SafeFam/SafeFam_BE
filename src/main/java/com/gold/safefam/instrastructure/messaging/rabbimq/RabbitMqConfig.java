package com.gold.safefam.instrastructure.messaging.rabbimq;

import org.springframework.amqp.core.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig {

    // Exchange 생성
    @Bean
    TopicExchange analysisExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.excahnge(), true, false);
    }

    // Queue 생성
    @Bean
    Queue analysisRequestQueue(RabbitMqProperties properties) {
        return QueueBuilder
                .durable(properties.requestQueue())
                .build();
    }

    // Binding 설정
    @Bean
    Binding analysisRequestBinding(
            Queue analysisRequestQueue,
            TopicExchange analysisExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder
                .bind(analysisRequestQueue)
                .to(analysisExchange)
                .with(properties.requestRoutingKey());
    }
}
