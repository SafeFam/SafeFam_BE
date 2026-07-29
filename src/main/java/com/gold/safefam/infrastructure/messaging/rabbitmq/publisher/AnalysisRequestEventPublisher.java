package com.gold.safefam.infrastructure.messaging.rabbitmq.publisher;

import com.gold.safefam.infrastructure.messaging.outbox.model.ClaimedOutboxEvent;
import com.gold.safefam.infrastructure.messaging.outbox.service.OutboxPayloadCipher;
import com.gold.safefam.infrastructure.messaging.rabbitmq.config.RabbitMqProperties;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class AnalysisRequestEventPublisher {

    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;
    private final OutboxPayloadCipher payloadCipher;

    public AnalysisRequestEventPublisher(
            RabbitTemplate rabbitTemplate,
            RabbitMqProperties rabbitMqProperties,
            OutboxPayloadCipher payloadCipher
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMqProperties = rabbitMqProperties;
        this.payloadCipher = payloadCipher;
    }

    public void publish(ClaimedOutboxEvent outboxEvent) {

        // DB Outbox 테이블에 암호화되어 있던 페이로드를 eventId로 복호화
        String payload = payloadCipher.decrypt(
                outboxEvent.encryptedPayload(),
                outboxEvent.eventId()
        );

        // RabbitMQ 메시지 객체 생성 및 메타데이터 설정
        Message message = MessageBuilder
                .withBody(payload.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(outboxEvent.eventId().toString())
                .setHeader(
                        "schemaVersion",
                        outboxEvent.schemaVersion()
                )
                .build();

        // Publisher Confirm 추적을 위한 CorrelationData 생성
        CorrelationData correlationData =
                new CorrelationData(
                        outboxEvent.eventId().toString()
                );

        // RabbitMQ Exchange로 메시지 발행
        rabbitTemplate.send(
                rabbitMqProperties.exchange(),
                rabbitMqProperties.requestRoutingKey(),
                message,
                correlationData
        );

        CorrelationData.Confirm confirm;

        // 동기식 Publisher Confirm 확인
        try {
            confirm = correlationData
                    .getFuture()
                    .get(
                            CONFIRM_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "RabbitMQ publisher confirm timeout",
                    exception
            );
        }

        // RabbitMQ 브로커 차원의 거부(NACK) 처리
        if (!confirm.ack()) {
            throw new IllegalStateException(
                    "RabbitMQ rejected the message: "
                            + confirm.reason()
            );
        }

        // Unrouted 메시지 감지
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException(
                    "RabbitMQ message was not routed to a queue"
            );
        }
    }
}