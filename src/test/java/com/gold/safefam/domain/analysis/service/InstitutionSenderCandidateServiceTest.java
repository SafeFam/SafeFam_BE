package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.entity.InstitutionSenderCandidate;
import com.gold.safefam.domain.analysis.enums.SenderCandidateStatus;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;
import com.gold.safefam.domain.analysis.repository.InstitutionSenderCandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstitutionSenderCandidateServiceTest {

    private static final OffsetDateTime FIRST_SEEN =
            OffsetDateTime.of(
                    2026, 8, 14, 9, 0, 0, 0, ZoneOffset.UTC
            );

    private InstitutionSenderCandidateRepository repository;
    private InstitutionSenderCandidateService service;

    @BeforeEach
    void setUp() {
        repository = mock(InstitutionSenderCandidateRepository.class);
        service = new InstitutionSenderCandidateService(
                repository,
                new SenderNumberNormalizer(),
                3
        );
    }

    @Test
    void savesCandidateWhenInstitutionWasDetectedWithoutUrlComparison() {
        AnalysisResultEvent.Payload.InstitutionMatch institutionMatch =
                institutionMatch("국민은행", false);
        when(repository.findByInstitutionAndNormalizedSender(
                "국민은행",
                "15889999"
        )).thenReturn(Optional.empty());

        service.recordCandidate(
                101L,
                "1588-9999",
                institutionMatch,
                FIRST_SEEN
        );

        ArgumentCaptor<InstitutionSenderCandidate> captor =
                ArgumentCaptor.forClass(InstitutionSenderCandidate.class);
        verify(repository).save(captor.capture());
        InstitutionSenderCandidate saved = captor.getValue();
        assertEquals("국민은행", saved.getInstitution());
        assertEquals("15889999", saved.getNormalizedSender());
        assertEquals(1, saved.getObservationCount());
        assertEquals(SenderCandidateStatus.COLLECTING, saved.getStatus());
        assertEquals(101L, saved.getLastAnalysisId());
    }

    @Test
    void movesRepeatedCandidateToReviewQueueAtThreshold() {
        InstitutionSenderCandidate candidate =
                new InstitutionSenderCandidate(
                        "국민은행",
                        "15889999",
                        101L,
                        FIRST_SEEN,
                        3
                );
        candidate.observe(102L, FIRST_SEEN.plusHours(1), 3);
        when(repository.findByInstitutionAndNormalizedSender(
                "국민은행",
                "15889999"
        )).thenReturn(Optional.of(candidate));

        service.recordCandidate(
                103L,
                "1588-9999",
                institutionMatch("국민은행", true),
                FIRST_SEEN.plusHours(2)
        );

        assertEquals(3, candidate.getObservationCount());
        assertEquals(
                SenderCandidateStatus.REVIEW_REQUIRED,
                candidate.getStatus()
        );
        assertEquals(103L, candidate.getLastAnalysisId());
        verify(repository, never()).save(candidate);
    }

    @Test
    void ignoresNonNumericSenderAndMissingInstitution() {
        service.recordCandidate(
                101L,
                "국민은행",
                institutionMatch("국민은행", false),
                FIRST_SEEN
        );
        service.recordCandidate(
                102L,
                "1588-9999",
                institutionMatch(null, false),
                FIRST_SEEN
        );

        verify(repository, never())
                .findByInstitutionAndNormalizedSender(
                        anyString(),
                        anyString()
                );
        verify(repository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
    }

    private AnalysisResultEvent.Payload.InstitutionMatch institutionMatch(
            String institution,
            boolean checked
    ) {
        return new AnalysisResultEvent.Payload.InstitutionMatch(
                checked,
                false,
                institution,
                List.of("kbstar.com"),
                null
        );
    }
}
