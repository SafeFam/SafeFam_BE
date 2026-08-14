package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.event.AnalysisResultCommittedEvent;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.infrastructure.messaging.idempotency.ProcessedAnalysisEventRepository;
import com.gold.safefam.support.AnalysisResultEventFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisResultApplyMappingTest {

    private AnalysisRepository analysisRepository;
    private ProcessedAnalysisEventRepository processedEventRepository;
    private InstitutionSenderCandidateService senderCandidateService;
    private ApplicationEventPublisher eventPublisher;
    private AnalysisResultApplyService service;

    @BeforeEach
    void setUp() {
        analysisRepository = mock(AnalysisRepository.class);
        processedEventRepository =
                mock(ProcessedAnalysisEventRepository.class);
        senderCandidateService =
                mock(InstitutionSenderCandidateService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new AnalysisResultApplyService(
                analysisRepository,
                processedEventRepository,
                new AnalysisResultValidator(),
                senderCandidateService,
                eventPublisher
        );
    }

    @Test
    void appliesCompletedResultAndDetails() {
        Analysis analysis = pending();
        AnalysisResultEvent event = AnalysisResultEventFixture.completed();
        prepareNewEvent(analysis);

        assertEquals(
                AnalysisResultApplyService.ApplyResult.APPLIED,
                service.apply(event)
        );
        assertEquals(AnalysisStatus.COMPLETED, analysis.getStatus());
        assertEquals(90, analysis.getRawTextScore());
        assertEquals(50, analysis.getLlmScore());
        assertEquals(82, analysis.getTotalScore());
        assertEquals(RiskLevel.HIGH, analysis.getRiskLevel());
        assertEquals(1, analysis.getUrlRisks().size());
        assertEquals(2, analysis.getEvidenceCards().size());
        assertEquals(
                "DANGEROUS_URL",
                analysis.getEvidenceCards().get(0).getCategory()
        );
        assertEquals(
                "위험 URL",
                analysis.getEvidenceCards().get(0).getTitle()
        );
        assertEquals(4, analysis.getIndicators().size());
        verify(eventPublisher).publishEvent(
                any(AnalysisResultCommittedEvent.class)
        );
    }

    @Test
    void appliesPartialResultAndRecordsFailedTrack() {
        Analysis analysis = pending();
        prepareNewEvent(analysis);

        service.apply(AnalysisResultEventFixture.partial());

        assertEquals(
                AnalysisStatus.PARTIAL_SUCCESS,
                analysis.getStatus()
        );
        assertEquals(
                1,
                analysis.getIndicators().stream()
                        .filter(indicator -> indicator.getType()
                                == IndicatorType.ANALYSIS_TRACK_FAILURE)
                        .count()
        );
    }

    @Test
    void mapsInstitutionDomainMismatchToImpersonationIndicator() {
        Analysis analysis = pending();
        prepareNewEvent(analysis);

        service.apply(
                AnalysisResultEventFixture.institutionDomainMismatch()
        );

        assertTrue(analysis.getIndicators().stream()
                .anyMatch(indicator -> indicator.getType()
                        == IndicatorType.IMPERSONATION
                        && indicator.getDescription().contains("KB국민은행")
                        && indicator.getDescription().contains("fake-kb.example")
                        && indicator.getDescription().contains("kbstar.com")));
    }

    @Test
    void doesNotMapSkippedInstitutionComparisonToImpersonationIndicator() {
        Analysis analysis = pending();
        prepareNewEvent(analysis);

        service.apply(
                AnalysisResultEventFixture.institutionComparisonSkipped()
        );

        assertFalse(analysis.getIndicators().stream()
                .anyMatch(indicator -> indicator.getType()
                        == IndicatorType.IMPERSONATION));
        verify(senderCandidateService).recordCandidate(
                eq(analysis.getId()),
                eq(analysis.getSender()),
                eq(AnalysisResultEventFixture
                        .institutionComparisonSkipped()
                        .payload()
                        .ruleAnalysis()
                        .institutionMatch()),
                any()
        );
    }

    @Test
    void appliesFailedResultWithoutInventingRiskScore() {
        Analysis analysis = pending();
        prepareNewEvent(analysis);

        service.apply(AnalysisResultEventFixture.failed());

        assertEquals(AnalysisStatus.FAILED, analysis.getStatus());
        assertEquals("PIPELINE_ERROR", analysis.getFailureCode());
        assertNull(analysis.getTotalScore());
        assertNull(analysis.getRiskLevel());
    }

    @Test
    void skipsAlreadyProcessedEvent() {
        when(processedEventRepository.insertIfAbsent(
                any(), anyLong(), anyString(), any()
        )).thenReturn(0);

        assertEquals(
                AnalysisResultApplyService.ApplyResult.DUPLICATE,
                service.apply(AnalysisResultEventFixture.completed())
        );
        verify(analysisRepository, never()).findByIdForUpdate(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private void prepareNewEvent(Analysis analysis) {
        when(processedEventRepository.insertIfAbsent(
                any(), anyLong(), anyString(), any()
        )).thenReturn(1);
        when(analysisRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(analysis));
    }

    private Analysis pending() {
        return Analysis.pending(
                10L,
                "message-1",
                "01012345678",
                "a".repeat(64),
                "preview",
                AnalysisSource.MANUAL,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
