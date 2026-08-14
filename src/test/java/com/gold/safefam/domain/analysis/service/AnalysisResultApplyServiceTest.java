package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisEventType;
import com.gold.safefam.domain.analysis.messaging.event.AnalysisResultEvent;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.infrastructure.messaging.idempotency.ProcessedAnalysisEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisResultApplyServiceTest {

    @Test
    void rejectsDifferentResultEventAfterAnalysisReachedTerminalState() {
        AnalysisRepository analysisRepository =
                mock(AnalysisRepository.class);
        ProcessedAnalysisEventRepository processedEventRepository =
                mock(ProcessedAnalysisEventRepository.class);
        AnalysisResultValidator validator =
                mock(AnalysisResultValidator.class);
        ApplicationEventPublisher eventPublisher =
                mock(ApplicationEventPublisher.class);
        InstitutionSenderCandidateService senderCandidateService =
                mock(InstitutionSenderCandidateService.class);
        AnalysisResultApplyService service =
                new AnalysisResultApplyService(
                        analysisRepository,
                        processedEventRepository,
                        validator,
                        senderCandidateService,
                        eventPublisher
                );

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);
        Analysis completed = new Analysis(
                1L,
                "message-1",
                "01012345678",
                "a".repeat(64),
                "분석 완료 문자",
                PhishingCategory.OTHER,
                AnalysisSource.MANUAL,
                10,
                20,
                30,
                RiskLevel.LOW,
                "분석 완료",
                now,
                now
        );
        AnalysisResultEvent event = new AnalysisResultEvent(
                "1.0",
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                "message-1",
                UUID.randomUUID(),
                Instant.now(),
                AnalysisEventType.ANALYSIS_FAILED,
                null
        );

        when(processedEventRepository.insertIfAbsent(
                any(),
                anyLong(),
                anyString(),
                any()
        )).thenReturn(1);
        when(analysisRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(completed));

        assertThrows(
                AnalysisResultValidator
                        .InvalidAnalysisResultEventException.class,
                () -> service.apply(event)
        );

        verify(eventPublisher, never()).publishEvent(any());
    }
}
