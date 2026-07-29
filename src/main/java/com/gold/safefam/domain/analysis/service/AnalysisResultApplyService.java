package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisIndicator;
import com.gold.safefam.domain.analysis.entity.AnalysisUrlRisk;
import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.analysis.event.AnalysisResultCommittedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.gold.safefam.infrastructure.messaging.idempotency.ProcessedAnalysisEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
public class AnalysisResultApplyService {

    private final AnalysisRepository analysisRepository;
    private final ProcessedAnalysisEventRepository processedEventRepository;
    private final AnalysisResultValidator validator;
    private final ApplicationEventPublisher eventPublisher;

    public AnalysisResultApplyService(
            AnalysisRepository analysisRepository,
            ProcessedAnalysisEventRepository processedEventRepository,
            AnalysisResultValidator validator,
            ApplicationEventPublisher eventPublisher
    ) {
        this.analysisRepository = analysisRepository;
        this.processedEventRepository = processedEventRepository;
        this.validator = validator;
        this.eventPublisher = eventPublisher;
    }

    /* 분석 결과 이벤트 적용 */
    @Transactional
    public ApplyResult apply(AnalysisResultEvent event) {

        // 계약 유효성 검증
        validator.validate(event);

        // 멱등성 검증
        int inserted = processedEventRepository.insertIfAbsent(
                event.eventId(),
                event.analysisId(),
                event.eventType().name(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        if (inserted == 0) {
            return ApplyResult.DUPLICATE;
        }

        // 비관적 쓰기 잠금 (SELECT FOR UPDATE)
        Analysis analysis = analysisRepository
                .findByIdForUpdate(event.analysisId())
                .orElseThrow(() -> new UnknownAnalysisException(
                        event.analysisId()
                ));

        validateClientMessageId(analysis, event);

        // 상태별 도메인 엔티티 상태 엄데이트
        switch (event.eventType()) {
            case ANALYSIS_COMPLETED ->
                    applySuccessfulResult(
                            analysis,
                            event,
                            AnalysisStatus.COMPLETED
                    );

            case ANALYSIS_PARTIAL ->
                    applySuccessfulResult(
                            analysis,
                            event,
                            AnalysisStatus.PARTIAL_SUCCESS
                    );

            case ANALYSIS_FAILED ->
                    applyFailedResult(analysis, event);
        }

        eventPublisher.publishEvent(
                AnalysisResultCommittedEvent.from(analysis)
        );

        return ApplyResult.APPLIED;
    }

    /* 성공 / 부분 성공 결과 엔티티 반영 */
    private void applySuccessfulResult(
            Analysis analysis,
            AnalysisResultEvent event,
            AnalysisStatus status
    ) {
        AnalysisResultEvent.Payload payload = event.payload();
        AnalysisResultEvent.Payload.RawScores rawScores =
                payload.rawScores();
        AnalysisResultEvent.Payload.WeightedContributions weighted =
                payload.weightedContributions();

        analysis.clearResultDetails();

        analysis.complete(
                status,
                rawScores.text(),
                rawScores.url(),
                rawScores.rules(),
                weighted.text(),
                weighted.url(),
                weighted.rules(),
                payload.finalScore(),
                payload.riskGrade(),
                toPhishingCategory(payload.phishingType()),
                findExplanation(payload),
                toOffsetDateTime(event)
        );

        addTextEvidence(analysis, payload);
        addFailedTrackIndicators(analysis, payload);
        addUrlResult(analysis, payload);
        addRuleIndicators(analysis, payload);
    }

    /* 분석 실패 결과 엔티티 반영 */
    private void applyFailedResult(
            Analysis analysis,
            AnalysisResultEvent event
    ) {
        analysis.clearResultDetails();

        analysis.fail(
                event.payload().failureCode(),
                toOffsetDateTime(event)
        );

        addFailedTrackIndicators(analysis, event.payload());
    }

    /* 텍스트(LLM/NLP) 근거 및 판단 사유를 분석 지표로 추가 */
    private void addTextEvidence(
            Analysis analysis,
            AnalysisResultEvent.Payload payload
    ) {
        AnalysisResultEvent.Payload.TextAnalysis textAnalysis =
                payload.textAnalysis();

        if (textAnalysis == null) {
            return;
        }

        // 근거 문장 추가
        for (String evidence : safeList(textAnalysis.evidence())) {
            if (hasText(evidence)) {
                analysis.addIndicator(new AnalysisIndicator(
                        IndicatorType.AI_EVIDENCE,
                        truncate(evidence, 500)
                ));
            }
        }

        // 목록이 비어있어도 정보가 있다면 Indicator로 보존
        if (safeList(textAnalysis.evidence()).isEmpty()
                && hasText(textAnalysis.reason())) {
            analysis.addIndicator(new AnalysisIndicator(
                    IndicatorType.AI_EVIDENCE,
                    truncate(textAnalysis.reason(), 500)
            ));
        }
    }

    /* 분석 결과 중 실패한 트랙을 지표로 기록 */
    private void addFailedTrackIndicators(
            Analysis analysis,
            AnalysisResultEvent.Payload payload
    ) {
        for (String failedTrack : safeList(payload.failedTracks())) {
            if (hasText(failedTrack)) {
                analysis.addIndicator(new AnalysisIndicator(
                        IndicatorType.ANALYSIS_TRACK_FAILURE,
                        truncate(
                                "Analysis track unavailable: "
                                        + failedTrack,
                                500
                        )
                ));
            }
        }
    }

    private void addUrlResult(
            Analysis analysis,
            AnalysisResultEvent.Payload payload
    ) {
        AnalysisResultEvent.Payload.UrlAnalysis url =
                payload.urlAnalysis();

        if (url == null || !url.hasUrl()) {
            return;
        }

        /*
         * hasUrl=true인데 originalUrl이 없다면 엔티티의 NOT NULL 제약을
         * 위반하므로 Validator에서 막는 것도 가능하다.
         * 여기서는 방어적으로 빈 값 저장을 막는다.
         */
        if (!hasText(url.originalUrl())) {
            throw new AnalysisResultValidator
                    .InvalidAnalysisResultEventException(
                    "urlAnalysis.originalUrl is required when hasUrl is true"
            );
        }

        analysis.addUrlRisk(new AnalysisUrlRisk(
                url.originalUrl(),
                url.tracedUrl(),
                url.malicious(),
                url.score(),
                url.engineSource(),
                url.errorCode()
        ));

        if (Boolean.TRUE.equals(url.malicious())) {
            analysis.addIndicator(new AnalysisIndicator(
                    IndicatorType.MALICIOUS_URL,
                    "Malicious URL detected"
            ));
        } else if (url.tracedUrl() != null
                && !url.originalUrl().equals(url.tracedUrl())) {
            analysis.addIndicator(new AnalysisIndicator(
                    IndicatorType.SHORTENED_URL,
                    "Shortened URL destination was traced"
            ));
        }
    }

    private void addRuleIndicators(
            Analysis analysis,
            AnalysisResultEvent.Payload payload
    ) {
        AnalysisResultEvent.Payload.RuleAnalysis ruleAnalysis =
                payload.ruleAnalysis();

        if (ruleAnalysis == null) {
            return;
        }

        /*
         * matchedRules의 문자열은 Spring IndicatorType과 같은 계약이
         * 보장되지 않으므로 억지로 enum으로 변환하지 않는다.
         *
         * 현재는 AI_EVIDENCE로 저장한다.
         */
        for (String matchedRule : safeList(ruleAnalysis.matchedRules())) {
            if (hasText(matchedRule)) {
                analysis.addIndicator(new AnalysisIndicator(
                        IndicatorType.AI_EVIDENCE,
                        truncate(
                                "Matched rule: " + matchedRule,
                                500
                        )
                ));
            }
        }

        if (ruleAnalysis.maliciousDomainPattern()) {
            analysis.addIndicator(new AnalysisIndicator(
                    IndicatorType.MALICIOUS_URL,
                    "Malicious domain pattern detected"
            ));
        }
    }

    private String findExplanation(
            AnalysisResultEvent.Payload payload
    ) {
        if (payload.textAnalysis() == null) {
            return null;
        }

        if (hasText(payload.textAnalysis().reason())) {
            return payload.textAnalysis().reason();
        }

        List<String> evidence = safeList(
                payload.textAnalysis().evidence()
        );

        return evidence.stream()
                .filter(this::hasText)
                .findFirst()
                .orElse(null);
    }

    private PhishingCategory toPhishingCategory(String phishingType) {
        /*
         * AI가 아직 phishingType을 계산하지 않으므로 null은 그대로 저장한다.
         */
        if (!hasText(phishingType)) {
            return null;
        }

        try {
            return PhishingCategory.valueOf(phishingType);
        } catch (IllegalArgumentException exception) {
            /*
             * BE enum과 일치하지 않는 새 값이 도착하더라도
             * 메시지 전체를 재시도하지 않고 OTHER로 안전하게 수렴시킨다.
             */
            return PhishingCategory.OTHER;
        }
    }

    private void validateClientMessageId(
            Analysis analysis,
            AnalysisResultEvent event
    ) {
        /*
         * 양쪽 값이 모두 존재할 때만 비교한다.
         * clientMessageId는 선택값이기 때문이다.
         */
        if (analysis.getClientMessageId() != null
                && event.clientMessageId() != null
                && !Objects.equals(
                analysis.getClientMessageId(),
                event.clientMessageId()
        )) {
            throw new AnalysisResultValidator
                    .InvalidAnalysisResultEventException(
                    "clientMessageId does not match analysis"
            );
        }
    }

    private OffsetDateTime toOffsetDateTime(
            AnalysisResultEvent event
    ) {
        return event.occurredAt()
                .atOffset(ZoneOffset.UTC);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String truncate(String value, int maximumLength) {
        if (value.length() <= maximumLength) {
            return value;
        }

        return value.substring(0, maximumLength);
    }

    public enum ApplyResult {
        APPLIED,
        DUPLICATE
    }

    public static class UnknownAnalysisException
            extends RuntimeException {

        public UnknownAnalysisException(Long analysisId) {
            super("Analysis not found: " + analysisId);
        }
    }
}