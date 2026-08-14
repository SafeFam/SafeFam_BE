package com.gold.safefam.domain.analysis.mapper;

import com.gold.safefam.domain.analysis.dto.AnalysisResponse;
import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisEvidenceCard;
import com.gold.safefam.domain.analysis.entity.AnalysisIndicator;
import com.gold.safefam.domain.analysis.entity.AnalysisUrlRisk;
import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.service.AnalysisResultFactory;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisResponseMapperTest {

    @Test
    void fallsBackToOriginalUrlWhenTraceWasNotPerformed() {
        AnalysisResultFactory resultFactory =
                mock(AnalysisResultFactory.class);
        when(resultFactory.recommendedActionsFor(
                RiskLevel.HIGH,
                PhishingCategory.OTHER
        )).thenReturn(List.of());

        AnalysisResponseMapper mapper =
                new AnalysisResponseMapper(resultFactory);
        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);
        Analysis analysis = new Analysis(
                1L,
                "message-1",
                "01012345678",
                "a".repeat(64),
                "의심 문자",
                PhishingCategory.OTHER,
                AnalysisSource.MANUAL,
                80,
                70,
                75,
                RiskLevel.HIGH,
                "위험 URL이 포함되어 있습니다.",
                now,
                now
        );
        analysis.addUrlRisk(new AnalysisUrlRisk(
                "https://example.test/login",
                false,
                true
        ));

        AnalysisResponse response = mapper.toResponse(analysis);

        assertEquals(1, response.urls().size());
        assertEquals(
                "https://example.test/login",
                response.urls().get(0).resolvedUrl()
        );
        assertEquals(0, response.scoreBreakdown().textScore());
        assertEquals(80, response.scoreBreakdown().urlScore());
        assertEquals(70, response.scoreBreakdown().rulesScore());
    }

    @Test
    void exposesFailedTracksSeparatelyFromIndicators() {
        AnalysisResultFactory resultFactory =
                mock(AnalysisResultFactory.class);
        when(resultFactory.recommendedActionsFor(
                RiskLevel.HIGH,
                PhishingCategory.OTHER
        )).thenReturn(List.of());

        AnalysisResponseMapper mapper =
                new AnalysisResponseMapper(resultFactory);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Analysis analysis = new Analysis(
                1L, "message-1", "15889999", "a".repeat(64),
                "의심 문자", PhishingCategory.OTHER,
                AnalysisSource.MANUAL, 80, 70, 75,
                RiskLevel.HIGH, "일부 분석을 사용할 수 없습니다.",
                now, now
        );
        analysis.addIndicator(new AnalysisIndicator(
                IndicatorType.ANALYSIS_TRACK_FAILURE,
                "Analysis track unavailable: TEXT:LLM"
        ));

        AnalysisResponse response = mapper.toResponse(analysis);

        assertEquals(List.of("TEXT:LLM"), response.failedTracks());
    }

    @Test
    void exposesEvidenceCardsAndRawTrackScores() {
        AnalysisResultFactory resultFactory =
                mock(AnalysisResultFactory.class);
        when(resultFactory.recommendedActionsFor(
                RiskLevel.HIGH,
                PhishingCategory.OTHER
        )).thenReturn(List.of());

        AnalysisResponseMapper mapper =
                new AnalysisResponseMapper(resultFactory);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Analysis analysis = Analysis.pending(
                1L,
                "message-1",
                "15889999",
                "a".repeat(64),
                "피싱 의심 문자",
                AnalysisSource.MANUAL,
                now
        );
        analysis.complete(
                com.gold.safefam.domain.analysis.enums.AnalysisStatus.COMPLETED,
                88,
                95,
                80,
                44,
                28,
                16,
                88,
                RiskLevel.HIGH,
                PhishingCategory.OTHER,
                "위험 신호가 감지됐습니다.",
                now
        );
        analysis.addEvidenceCard(new AnalysisEvidenceCard(
                "URGENCY_PRESSURE",
                "행동 압박",
                "빠른 판단을 재촉하는 표현이 있습니다."
        ));

        AnalysisResponse response = mapper.toResponse(analysis);

        assertEquals(88, response.scoreBreakdown().textScore());
        assertEquals(95, response.scoreBreakdown().urlScore());
        assertEquals(80, response.scoreBreakdown().rulesScore());
        assertEquals(1, response.evidenceCards().size());
        assertEquals(
                "URGENCY_PRESSURE",
                response.evidenceCards().get(0).category()
        );
        assertEquals(
                "행동 압박",
                response.evidenceCards().get(0).title()
        );
    }
}
