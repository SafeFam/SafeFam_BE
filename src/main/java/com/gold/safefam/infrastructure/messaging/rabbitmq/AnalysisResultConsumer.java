package com.gold.safefam.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;
import com.gold.safefam.domain.analysis.service.AnalysisResultApplyService;
import com.gold.safefam.domain.analysis.service.AnalysisResultValidator;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AnalysisResultConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(AnalysisResultConsumer.class);

    private final ObjectMapper objectMapper;
    private final AnalysisResultApplyService applyService;

    public AnalysisResultConsumer(
            ObjectMapper objectMapper,
            AnalysisResultApplyService applyService
    ) {
        this.objectMapper = objectMapper;
        this.applyService = applyService;
    }

    /* 분석 결과 메시지 수신 및 처리 리스너 */
    @RabbitListener(
            queues = "${safefam.messaging.analysis.result-queue}",
            containerFactory = "analysisResultListenerContainerFactory",
            autoStartup = "${safefam.messaging.analysis.consumer-enabled:true}"
    )
    public void consume(
            Message message,
            Channel channel
    ) throws IOException {
        long deliveryTag =
                message.getMessageProperties().getDeliveryTag();

        AnalysisResultEvent event;

        // JSON 역직렬화
        try {
            event = objectMapper.readValue(
                    message.getBody(),
                    AnalysisResultEvent.class
            );
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Rejecting malformed analysis result message. "
                            + "deliveryTag={}, body={}",
                    deliveryTag,
                    safeBody(message),
                    exception
            );

            channel.basicReject(deliveryTag, false);
            return;
        }

        try {
            // 비즈니스 로직 적용 (멱등성 검증, DB 반영, 내부 이벤트 발행)
            AnalysisResultApplyService.ApplyResult result =
                    applyService.apply(event);

            // 정상 처리 완료 승인 (ACK)
            channel.basicAck(deliveryTag, false);

            log.info(
                    "Analysis result consumed. "
                            + "result={}, eventId={}, analysisId={}, "
                            + "traceId={}, eventType={}",
                    result,
                    event.eventId(),
                    event.analysisId(),
                    event.traceId(),
                    event.eventType()
            );
        } catch (
                AnalysisResultValidator.InvalidAnalysisResultEventException
                | AnalysisResultApplyService.UnknownAnalysisException
                        exception
        ) {
            // 예외 처리 A = 비가역적 오류 (Bad Request / Data Unmatched)
            log.warn(
                    "Rejecting invalid analysis result event. "
                            + "eventId={}, analysisId={}, traceId={}, reason={}",
                    event.eventId(),
                    event.analysisId(),
                    event.traceId(),
                    exception.getMessage()
            );

            channel.basicReject(deliveryTag, false);
        } catch (DataAccessException exception) {

            // 예외 처리 B - 일시적 DB 장애 (Transient Failure)
            log.error(
                    "Transient database failure while applying analysis result. "
                            + "eventId={}, analysisId={}, traceId={}",
                    event.eventId(),
                    event.analysisId(),
                    event.traceId(),
                    exception
            );

            channel.basicNack(deliveryTag, false, true);
        } catch (RuntimeException exception) {

            // 예외 처리 C - 기타 시스템 예외
            log.error(
                    "Unexpected failure while applying analysis result. "
                            + "eventId={}, analysisId={}, traceId={}",
                    event.eventId(),
                    event.analysisId(),
                    event.traceId(),
                    exception
            );

            channel.basicNack(deliveryTag, false, true);
        }
    }

    private String safeBody(Message message) {
        String body = new String(
                message.getBody(),
                StandardCharsets.UTF_8
        );

        if (body.length() <= 500) {
            return body;
        }

        return body.substring(0, 500) + "...";
    }
}