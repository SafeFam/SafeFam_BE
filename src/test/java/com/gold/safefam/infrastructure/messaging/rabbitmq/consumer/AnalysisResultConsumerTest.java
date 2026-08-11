package com.gold.safefam.infrastructure.messaging.rabbitmq.consumer;

import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;
import com.gold.safefam.domain.analysis.service.AnalysisResultApplyService;
import com.gold.safefam.domain.analysis.service.AnalysisResultValidator;
import com.gold.safefam.support.AnalysisResultEventFixture;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.dao.TransientDataAccessResourceException;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class AnalysisResultConsumerTest {

    private ObjectMapper objectMapper;
    private AnalysisResultApplyService applyService;
    private Channel channel;
    private AnalysisResultConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        applyService = mock(AnalysisResultApplyService.class);
        channel = mock(Channel.class);
        consumer = new AnalysisResultConsumer(objectMapper, applyService);
    }

    @Test
    void acknowledgesAppliedEvent() throws Exception {
        AnalysisResultEvent event = AnalysisResultEventFixture.completed();
        when(applyService.apply(event))
                .thenReturn(AnalysisResultApplyService.ApplyResult.APPLIED);

        consumer.consume(message(event, 11L), channel);

        verify(channel).basicAck(11L, false);
        verify(channel, never()).basicReject(11L, false);
    }

    @Test
    void deserializesHybridTextAnalysisFields() throws Exception {
        AnalysisResultEvent expected =
                AnalysisResultEventFixture.completed();

        AnalysisResultEvent restored = objectMapper.readValue(
                objectMapper.writeValueAsBytes(expected),
                AnalysisResultEvent.class
        );

        AnalysisResultEvent.Payload.TextAnalysis text =
                restored.payload().textAnalysis();

        assertThat(text.method())
                .isEqualTo("STACKING_LLM");

        assertThat(text.selfModelScore())
                .isEqualTo(70);

        assertThat(text.selfModelConfidence())
                .isEqualTo(0.72);

        assertThat(text.llmCalled())
                .isTrue();

        assertThat(text.llmProvider())
                .isEqualTo("AWS_BEDROCK");

        assertThat(text.llmModel())
                .isEqualTo(
                        "anthropic.claude-haiku-4-5-20251001-v1:0"
                );

        assertThat(text.resolvedLlmCalled())
                .isTrue();

        assertThat(text.geminiCalled())
                .isTrue();

        assertThat(text.decisionSource())
                .isEqualTo("LLM");

        assertThat(text.routingReason())
                .isEqualTo(
                        "UNCERTAIN_SELF_MODEL_PREDICTION"
                );

        assertThat(text.fallbackApplied())
                .isFalse();
    }

    @Test
    void acknowledgesDuplicateEvent() throws Exception {
        AnalysisResultEvent event = AnalysisResultEventFixture.completed();
        when(applyService.apply(event))
                .thenReturn(AnalysisResultApplyService.ApplyResult.DUPLICATE);

        consumer.consume(message(event, 12L), channel);

        verify(channel).basicAck(12L, false);
    }

    @Test
    void rejectsMalformedJsonWithoutRequeue() throws Exception {
        Message message = MessageBuilder
                .withBody("{invalid-json".getBytes())
                .setDeliveryTag(13L)
                .build();

        consumer.consume(message, channel);

        verify(channel).basicReject(13L, false);
        verify(channel, never()).basicAck(13L, false);
    }

    @Test
    void rejectsInvalidBusinessEventWithoutRequeue() throws Exception {
        AnalysisResultEvent event = AnalysisResultEventFixture.completed();
        when(applyService.apply(event)).thenThrow(
                new AnalysisResultValidator
                        .InvalidAnalysisResultEventException("invalid")
        );

        consumer.consume(message(event, 14L), channel);

        verify(channel).basicReject(14L, false);
    }

    @Test
    void requeuesTransientDatabaseFailure() throws Exception {
        AnalysisResultEvent event = AnalysisResultEventFixture.completed();
        when(applyService.apply(event)).thenThrow(
                new TransientDataAccessResourceException("temporary")
        );

        consumer.consume(message(event, 15L), channel);

        verify(channel).basicNack(15L, false, true);
    }

    @Test
    void requeuesUnexpectedRuntimeFailure() throws Exception {
        AnalysisResultEvent event = AnalysisResultEventFixture.completed();
        when(applyService.apply(event))
                .thenThrow(new IllegalStateException("temporary"));

        consumer.consume(message(event, 16L), channel);

        verify(channel).basicNack(16L, false, true);
    }

    private Message message(
            AnalysisResultEvent event,
            long deliveryTag
    ) throws Exception {
        return MessageBuilder
                .withBody(objectMapper.writeValueAsBytes(event))
                .setDeliveryTag(deliveryTag)
                .build();
    }

    @Test
    void fallsBackToLegacyGeminiCalledField() throws Exception {
        String legacyJson = """
            {
              "method": "STACKING_GEMINI",
              "score": 85,
              "grade": "DANGEROUS",
              "reason": "기관 사칭이 감지되었습니다.",
              "evidence": [],
              "failedEngines": [],
              "selfModelScore": 60,
              "selfModelConfidence": 0.72,
              "geminiCalled": true,
              "decisionSource": "GEMINI",
              "routingReason": "UNCERTAIN_SELF_MODEL_PREDICTION",
              "fallbackApplied": false
            }
            """;

        AnalysisResultEvent.Payload.TextAnalysis text =
                objectMapper.readValue(
                        legacyJson,
                        AnalysisResultEvent.Payload.TextAnalysis.class
                );

        assertThat(text.llmCalled())
                .isNull();

        assertThat(text.llmProvider())
                .isNull();

        assertThat(text.llmModel())
                .isNull();

        assertThat(text.geminiCalled())
                .isTrue();

        assertThat(text.resolvedLlmCalled())
                .isTrue();
    }

    @Test
    void acceptsProviderNeutralLlmFieldsWithoutLegacyAlias()
            throws Exception {
        String newJson = """
            {
              "method": "STACKING_LLM",
              "score": 85,
              "grade": "DANGEROUS",
              "reason": "기관 사칭이 감지되었습니다.",
              "evidence": [],
              "failedEngines": [],
              "selfModelScore": 60,
              "selfModelConfidence": 0.72,
              "llmCalled": true,
              "llmProvider": "AWS_BEDROCK",
              "llmModel": "anthropic.claude-haiku-4-5-20251001-v1:0",
              "decisionSource": "LLM",
              "routingReason": "UNCERTAIN_SELF_MODEL_PREDICTION",
              "fallbackApplied": false
            }
            """;

        AnalysisResultEvent.Payload.TextAnalysis text =
                objectMapper.readValue(
                        newJson,
                        AnalysisResultEvent.Payload.TextAnalysis.class
                );

        assertThat(text.llmCalled())
                .isTrue();

        assertThat(text.llmProvider())
                .isEqualTo("AWS_BEDROCK");

        assertThat(text.geminiCalled())
                .isNull();

        assertThat(text.resolvedLlmCalled())
                .isTrue();
    }
}
