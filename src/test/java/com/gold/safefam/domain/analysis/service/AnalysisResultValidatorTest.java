package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.messaging.event.AnalysisEventType;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;
import com.gold.safefam.support.AnalysisResultEventFixture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisResultValidatorTest {

    private final AnalysisResultValidator validator =
            new AnalysisResultValidator();

    @Test
    void acceptsCompletedPartialAndFailedContracts() {
        assertDoesNotThrow(() -> validator.validate(
                AnalysisResultEventFixture.completed()
        ));
        assertDoesNotThrow(() -> validator.validate(
                AnalysisResultEventFixture.partial()
        ));
        assertDoesNotThrow(() -> validator.validate(
                AnalysisResultEventFixture.failed()
        ));
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        AnalysisResultEvent valid = AnalysisResultEventFixture.completed();
        AnalysisResultEvent invalid = new AnalysisResultEvent(
                "2.0", valid.eventId(), valid.causationId(),
                valid.analysisId(), valid.clientMessageId(),
                valid.traceId(), valid.occurredAt(),
                valid.eventType(), valid.payload()
        );

        assertThrows(
                AnalysisResultValidator
                        .InvalidAnalysisResultEventException.class,
                () -> validator.validate(invalid)
        );
    }

    @Test
    void rejectsCompletedEventWithFailedTracks() {
        AnalysisResultEvent.Payload payload =
                AnalysisResultEventFixture.successfulPayload(
                        List.of("URL")
                );

        assertThrows(
                AnalysisResultValidator
                        .InvalidAnalysisResultEventException.class,
                () -> validator.validate(
                        AnalysisResultEventFixture.event(
                                AnalysisEventType.ANALYSIS_COMPLETED,
                                payload
                        )
                )
        );
    }

    @Test
    void rejectsPartialEventWithoutFailedTrack() {
        assertThrows(
                AnalysisResultValidator
                        .InvalidAnalysisResultEventException.class,
                () -> validator.validate(
                        AnalysisResultEventFixture.event(
                                AnalysisEventType.ANALYSIS_PARTIAL,
                                AnalysisResultEventFixture
                                        .successfulPayload(List.of())
                        )
                )
        );
    }

    @Test
    void rejectsFailedEventWithoutFailureCode() {
        AnalysisResultEvent.Payload payload =
                new AnalysisResultEvent.Payload(
                        null, null, null, null, null,
                        null, null, null, List.of("PIPELINE"), null
                );

        assertThrows(
                AnalysisResultValidator
                        .InvalidAnalysisResultEventException.class,
                () -> validator.validate(
                        AnalysisResultEventFixture.event(
                                AnalysisEventType.ANALYSIS_FAILED,
                                payload
                        )
                )
        );
    }

    @Test
    void rejectsOutOfRangeScore() {
        AnalysisResultEvent.Payload valid =
                AnalysisResultEventFixture.successfulPayload(List.of());
        AnalysisResultEvent.Payload invalid =
                new AnalysisResultEvent.Payload(
                        101, valid.riskGrade(), valid.phishingType(),
                        valid.rawScores(),
                        valid.weightedContributions(),
                        valid.textAnalysis(), valid.urlAnalysis(),
                        valid.ruleAnalysis(), valid.failedTracks(),
                        valid.failureCode()
                );

        assertThrows(
                AnalysisResultValidator
                        .InvalidAnalysisResultEventException.class,
                () -> validator.validate(
                        AnalysisResultEventFixture.event(
                                AnalysisEventType.ANALYSIS_COMPLETED,
                                invalid
                        )
                )
        );
    }
}
