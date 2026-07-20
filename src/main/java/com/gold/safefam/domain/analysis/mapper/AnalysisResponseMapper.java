package com.gold.safefam.domain.analysis.mapper;

import com.gold.safefam.domain.analysis.dto.AnalysisResponse;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.Indicator;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.RecommendedAction;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.ScoreBreakdown;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.UrlThreat;
import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult;
import com.gold.safefam.domain.analysis.service.AnalysisResultFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 저장 엔티티를 외부 API 응답 DTO로 변환하는 전용 Mapper.
 * Service와 Controller가 JPA 하위 엔티티 구조나 DTO 생성 규칙을 알지 않게 한다.
 */
@Component
public class AnalysisResponseMapper {

    private final AnalysisResultFactory resultFactory;

    public AnalysisResponseMapper(AnalysisResultFactory resultFactory) {
        this.resultFactory = resultFactory;
    }

    /** DB에서 읽은 분석 Aggregate 전체를 AnalysisResponse 한 건으로 변환한다. */
    public AnalysisResponse toResponse(Analysis analysis) {
        List<Indicator> indicators = analysis.getIndicators().stream()
                .map(indicator -> new Indicator(indicator.getType(), indicator.getDescription()))
                .toList();
        List<UrlThreat> urls = analysis.getUrlRisks().stream()
                .map(url -> new UrlThreat(
                        url.getOriginalUrl(),
                        url.getOriginalUrl(),
                        url.isSuspicious()
                ))
                .toList();
        List<RecommendedAction> actions = resultFactory
                .recommendedActionsFor(analysis.getRiskLevel(), analysis.getCategory())
                .stream()
                .map(this::toRecommendedAction)
                .toList();

        return new AnalysisResponse(
                analysis.getId(),
                analysis.getTotalScore(),
                analysis.getRiskLevel(),
                analysis.getCategory(),
                analysis.getExplanation(),
                new ScoreBreakdown(
                        analysis.getLlmScore(),
                        analysis.getUrlScore(),
                        analysis.getPatternScore()
                ),
                indicators,
                urls,
                actions,
                analysis.getAnalyzedAt()
        );
    }

    private RecommendedAction toRecommendedAction(
            MessageRiskAnalysisResult.RecommendedAction action
    ) {
        return new RecommendedAction(
                action.type(),
                action.label(),
                action.phoneNumber(),
                action.url()
        );
    }
}
