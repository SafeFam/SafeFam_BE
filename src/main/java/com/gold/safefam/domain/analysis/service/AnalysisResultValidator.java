package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AnalysisResultValidator {

    private static final String SUPPORTED_SCHEMA_VERSION = "1.0";

    /* 이벤트 유효성 검증 */
    public void validate(AnalysisResultEvent event) {
        if (event == null) {
            throw new InvalidAnalysisResultEventException(
                    "Analysis result event must not be null"
            );
        }

        validateEnvelope(event);

        switch (event.eventType()) {
            case ANALYSIS_COMPLETED -> validateCompleted(event.payload());
            case ANALYSIS_PARTIAL -> validatePartial(event.payload());
            case ANALYSIS_FAILED -> validateFailed(event.payload());
            default -> throw new InvalidAnalysisResultEventException(
                    "Unsupported eventType: " + event.eventType()
            );
        }
    }

    /* 메시지 Envelope 필수 갑사 및 메타데이터 검증 */
    private void validateEnvelope(AnalysisResultEvent event) {
        if (!SUPPORTED_SCHEMA_VERSION.equals(event.schemaVersion())) {
            throw new InvalidAnalysisResultEventException(
                    "Unsupported schemaVersion: " + event.schemaVersion()
            );
        }

        if (event.eventId() == null) {
            throw new InvalidAnalysisResultEventException(
                    "eventId must not be null"
            );
        }

        if (event.causationId() == null) {
            throw new InvalidAnalysisResultEventException(
                    "causationId must not be null"
            );
        }

        if (event.analysisId() == null || event.analysisId() <= 0) {
            throw new InvalidAnalysisResultEventException(
                    "analysisId must be greater than zero"
            );
        }

        if (event.traceId() == null) {
            throw new InvalidAnalysisResultEventException(
                    "traceId must not be null"
            );
        }

        if (event.occurredAt() == null) {
            throw new InvalidAnalysisResultEventException(
                    "occurredAt must not be null"
            );
        }

        if (event.eventType() == null) {
            throw new InvalidAnalysisResultEventException(
                    "eventType must not be null"
            );
        }

        if (event.payload() == null) {
            throw new InvalidAnalysisResultEventException(
                    "payload must not be null"
            );
        }
    }

    /* 성공 상태 검증 */
    private void validateCompleted(AnalysisResultEvent.Payload payload) {
        validateSuccessfulResult(payload);

        if (!safeList(payload.failedTracks()).isEmpty()) {
            throw new InvalidAnalysisResultEventException(
                    "Completed event must not contain failedTracks"
            );
        }

        if (hasText(payload.failureCode())) {
            throw new InvalidAnalysisResultEventException(
                    "Completed event must not contain failureCode"
            );
        }
    }

    /* 부분 성공 상태 검증 */
    private void validatePartial(AnalysisResultEvent.Payload payload) {
        validateSuccessfulResult(payload);

        if (safeList(payload.failedTracks()).isEmpty()) {
            throw new InvalidAnalysisResultEventException(
                    "Partial event must contain at least one failed track"
            );
        }

        if (hasText(payload.failureCode())) {
            throw new InvalidAnalysisResultEventException(
                    "Partial event must not contain failureCode"
            );
        }
    }

    /* 실패 상태 검증 */
    private void validateFailed(AnalysisResultEvent.Payload payload) {
        if (!hasText(payload.failureCode())) {
            throw new InvalidAnalysisResultEventException(
                    "Failed event must contain failureCode"
            );
        }

        if (payload.finalScore() != null) {
            throw new InvalidAnalysisResultEventException(
                    "Failed event must not contain finalScore"
            );
        }

        if (payload.riskGrade() != null) {
            throw new InvalidAnalysisResultEventException(
                    "Failed event must not contain riskGrade"
            );
        }

        if (payload.weightedContributions() != null) {
            throw new InvalidAnalysisResultEventException(
                    "Failed event must not contain weightedContributions"
            );
        }
    }

    /* 성공/부분 성공 메시지의 공통 점수 필드 유요성 검증 */
    private void validateSuccessfulResult(
            AnalysisResultEvent.Payload payload
    ) {
        if (payload.finalScore() == null) {
            throw new InvalidAnalysisResultEventException(
                    "Successful event must contain finalScore"
            );
        }

        validateScore("finalScore", payload.finalScore());

        if (payload.riskGrade() == null) {
            throw new InvalidAnalysisResultEventException(
                    "Successful event must contain riskGrade"
            );
        }

        if (payload.rawScores() == null) {
            throw new InvalidAnalysisResultEventException(
                    "Successful event must contain rawScores"
            );
        }

        // 각 트랙별 Raw 점수 범위 검증
        validateNullableScore("rawScores.text", payload.rawScores().text());
        validateNullableScore("rawScores.url", payload.rawScores().url());
        validateNullableScore("rawScores.rules", payload.rawScores().rules());

        if (payload.weightedContributions() == null) {
            throw new InvalidAnalysisResultEventException(
                    "Successful event must contain weightedContributions"
            );
        }

        // 각 트랙별 가중 반영 점수 범위 검증
        validateNullableScore(
                "weightedContributions.text",
                payload.weightedContributions().text()
        );
        validateNullableScore(
                "weightedContributions.url",
                payload.weightedContributions().url()
        );
        validateNullableScore(
                "weightedContributions.rules",
                payload.weightedContributions().rules()
        );
    }

    /* Null이 아닌 점수 필드의 유효범위 검증 */
    private void validateNullableScore(String field, Integer score) {
        if (score != null) {
            validateScore(field, score);
        }
    }

    /* 점수 유효 범위(0~100) 검증 */
    private void validateScore(String field, int score) {
        if (score < 0 || score > 100) {
            throw new InvalidAnalysisResultEventException(
                    field + " must be between 0 and 100"
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    public static class InvalidAnalysisResultEventException
            extends RuntimeException {

        public InvalidAnalysisResultEventException(String message) {
            super(message);
        }
    }
}
