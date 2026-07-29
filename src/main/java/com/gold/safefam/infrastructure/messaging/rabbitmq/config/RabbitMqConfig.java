package com.gold.safefam.infrastructure.messaging.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig {

    // Topic Exchange 생성
    @Bean
    TopicExchange analysisExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    // 분석 요청 큐
    @Bean
    Queue analysisRequestQueue(RabbitMqProperties properties) {
        return QueueBuilder
                .durable(properties.requestQueue())
                .build();
    }

    // 분석 요청 큐 바인딩
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

    // 분석 결과 수신 큐
    @Bean
    Queue analysisResultQueue(
            RabbitMqProperties properties
    ) {
        return QueueBuilder
                .durable(properties.resultQueue())
                .withArgument("x-queue-type", "quorum")
                .withArgument(
                        "x-delivery-limit",
                        properties.resultMaxDeliveries()
                )
                .withArgument(
                        "x-dead-letter-exchange",
                        properties.exchange()
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        properties.resultDlqRoutingKey()
                )
                .build();
    }

    // 분석 완료 결과 큐 바인딩
    @Bean
    Binding completedResultBinding(
            Queue analysisResultQueue,
            TopicExchange analysisExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder
                .bind(analysisResultQueue)
                .to(analysisExchange)
                .with(properties.completedRoutingKey());
    }

    // 분석 부분 완료 결과 큐 바인딩
    @Bean
    Binding partialResultBinding(
            Queue analysisResultQueue,
            TopicExchange analysisExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder
                .bind(analysisResultQueue)
                .to(analysisExchange)
                .with(properties.partialRoutingKey());
    }

    // 분석 실패 결과 큐 바인딩
    @Bean
    Binding failedResultBinding(
            Queue analysisResultQueue,
            TopicExchange analysisExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder
                .bind(analysisResultQueue)
                .to(analysisExchange)
                .with(properties.failedRoutingKey());
    }

    // DLQ 생성
    @Bean
    Queue analysisResultDlq(
            RabbitMqProperties properties
    ) {
        return QueueBuilder
                .durable(properties.resultDlq())
                .build();
    }

    // DLQ 바인딩
    @Bean
    Binding analysisResultDlqBinding(
            Queue analysisResultDlq,
            TopicExchange analysisExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder
                .bind(analysisResultDlq)
                .to(analysisExchange)
                .with(properties.resultDlqRoutingKey());
    }
}
