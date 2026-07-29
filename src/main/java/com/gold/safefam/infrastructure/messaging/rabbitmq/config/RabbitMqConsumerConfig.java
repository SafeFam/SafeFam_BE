package com.gold.safefam.infrastructure.messaging.rabbitmq.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConsumerConfig {

    /* 분석 결과 전용 RabbitListener 컨테이너 팩토리 */
    @Bean
    SimpleRabbitListenerContainerFactory analysisResultListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            RabbitMqProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        configurer.configure(factory, connectionFactory);

        // 수동 ACK 모드 설정
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // Prefetch Count 설정
        factory.setPrefetchCount(properties.resultPrefetch());

        // 처리 실패 시 Requeue 금지
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}