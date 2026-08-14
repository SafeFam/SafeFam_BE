package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.entity.InstitutionSenderCandidate;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;
import com.gold.safefam.domain.analysis.repository.InstitutionSenderCandidateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

/** 기관명이 감지된 분석 요청의 발신번호를 사람 검토용 후보로 누적한다. */
@Service
public class InstitutionSenderCandidateService {

    private static final int MAX_INSTITUTION_LENGTH = 100;

    private final InstitutionSenderCandidateRepository repository;
    private final SenderNumberNormalizer normalizer;
    private final int reviewThreshold;

    public InstitutionSenderCandidateService(
            InstitutionSenderCandidateRepository repository,
            SenderNumberNormalizer normalizer,
            @Value(
                    "${safefam.analysis.sender-candidates.review-threshold:3}"
            ) int reviewThreshold
    ) {
        if (reviewThreshold < 1) {
            throw new IllegalArgumentException(
                    "Sender candidate review threshold must be positive"
            );
        }

        this.repository = repository;
        this.normalizer = normalizer;
        this.reviewThreshold = reviewThreshold;
    }

    public void recordCandidate(
            Long analysisId,
            String sender,
            AnalysisResultEvent.Payload.InstitutionMatch institutionMatch,
            OffsetDateTime observedAt
    ) {
        if (analysisId == null
                || institutionMatch == null
                || !hasText(institutionMatch.institution())
                || observedAt == null) {
            return;
        }

        String institution = institutionMatch.institution().trim();
        if (institution.length() > MAX_INSTITUTION_LENGTH) {
            return;
        }

        Optional<String> normalizedSender = normalizer.normalize(sender);
        if (normalizedSender.isEmpty()) {
            return;
        }

        repository.findByInstitutionAndNormalizedSender(
                institution,
                normalizedSender.get()
        ).ifPresentOrElse(
                candidate -> candidate.observe(
                        analysisId,
                        observedAt,
                        reviewThreshold
                ),
                () -> repository.save(new InstitutionSenderCandidate(
                        institution,
                        normalizedSender.get(),
                        analysisId,
                        observedAt,
                        reviewThreshold
                ))
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
