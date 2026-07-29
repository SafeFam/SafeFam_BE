package com.gold.safefam.domain.analysis.mapper;

import com.gold.safefam.domain.analysis.dto.AnalysisListItemResponse;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.Indicator;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.RecommendedAction;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.ScoreBreakdown;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.UrlThreat;
import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
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
        List<Indicator> indicators =
                analysis.getIndicators().stream()
                        .map(indicator -> new Indicator(
                                indicator.getType(),
                                indicator.getDescription()
                        ))
                        .toList();

        List<UrlThreat> urls =
                analysis.getUrlRisks().stream()
                        .map(url -> new UrlThreat(
                                url.getOriginalUrl(),
                                url.getTracedUrl() != null
                                        ? url.getTracedUrl()
                                        : url.getOriginalUrl(),
                                url.isSuspicious()
                        ))
                        .toList();

        List<RecommendedAction> actions =
                hasSuccessfulResult(analysis.getStatus())
                        ? resultFactory
                        .recommendedActionsFor(
                                analysis.getRiskLevel(),
                                analysis.getCategory()
                        )
                        .stream()
                        .map(this::toRecommendedAction)
                        .toList()
                        : List.of();

        return new AnalysisResponse(
                analysis.getId(),
                analysis.getStatus(),
                analysis.getTotalScore(),
                analysis.getRiskLevel(),
                analysis.getCategory(),
                analysis.getExplanation(),
                analysis.getFailureCode(),
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

    /** 목록 화면에 필요한 요약 정보만 반환하며 숫자형 발신자는 일부를 가린다. */
    public AnalysisListItemResponse toListItemResponse(Analysis analysis) {
        return new AnalysisListItemResponse(
                analysis.getId(),
                analysis.getStatus(),
                maskSender(analysis.getSender()),
                analysis.getContentPreview(),
                analysis.getTotalScore(),
                analysis.getRiskLevel(),
                analysis.getCategory(),
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

    private String maskSender(String sender) {
        if (sender == null || sender.isBlank()) {
            return sender;
        }

        String digits = sender.replaceAll("\\D", "");
        if (digits.length() == 10 || digits.length() == 11) {
            return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
        }
        if (digits.length() >= 8) {
            return digits.substring(0, 4) + "****";
        }
        return sender;
    }

    private boolean hasSuccessfulResult(
            AnalysisStatus status
    ) {
        return status == AnalysisStatus.COMPLETED
                || status == AnalysisStatus.PARTIAL_SUCCESS;
    }
}
