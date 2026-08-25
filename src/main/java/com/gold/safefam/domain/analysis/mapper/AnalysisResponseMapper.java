package com.gold.safefam.domain.analysis.mapper;

import com.gold.safefam.domain.analysis.dto.AnalysisListItemResponse;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.EvidenceCard;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.Indicator;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.RecommendedAction;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.ScoreBreakdown;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse.UrlThreat;
import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisIndicator;
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

    private static final String FAILED_TRACK_PREFIX =
            "Analysis track unavailable: ";
    private static final String MATCHED_RULE_PREFIX = "Matched rule: ";

    private final AnalysisResultFactory resultFactory;

    public AnalysisResponseMapper(AnalysisResultFactory resultFactory) {
        this.resultFactory = resultFactory;
    }

    /** DB에서 읽은 분석 Aggregate 전체를 AnalysisResponse 한 건으로 변환한다. */
    public AnalysisResponse toResponse(Analysis analysis) {
        List<Indicator> indicators =
                analysis.getIndicators().stream()
                        .filter(indicator -> indicator.getType()
                                != IndicatorType.ANALYSIS_TRACK_FAILURE)
                        .map(this::toPublicIndicator)
                        .toList();

        List<EvidenceCard> evidenceCards =
                analysis.getEvidenceCards().stream()
                        .map(card -> new EvidenceCard(
                                card.getCategory(),
                                card.getTitle(),
                                card.getDescription()
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

        List<String> failedTracks = analysis.getIndicators().stream()
                .filter(indicator -> indicator.getType()
                        == IndicatorType.ANALYSIS_TRACK_FAILURE)
                .map(indicator -> indicator.getDescription())
                .filter(description -> description != null)
                .map(this::normalizeFailedTrack)
                .filter(description -> !description.isBlank())
                .distinct()
                .toList();

        return new AnalysisResponse(
                analysis.getId(),
                analysis.getStatus(),
                analysis.getTotalScore(),
                analysis.getRiskLevel(),
                analysis.getCategory(),
                analysis.getExplanation(),
                analysis.getFailureCode(),
                failedTracks,
                new ScoreBreakdown(
                        rawScoreOrLegacyValue(
                                analysis.getRawTextScore(),
                                analysis.getLlmScore()
                        ),
                        rawScoreOrLegacyValue(
                                analysis.getRawUrlScore(),
                                analysis.getUrlScore()
                        ),
                        rawScoreOrLegacyValue(
                                analysis.getRawRulesScore(),
                                analysis.getPatternScore()
                        )
                ),
                evidenceCards,
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

    /** 실패 트랙은 전용 필드로만 노출하고, 일반 지표의 기존 영문 표현은 사용자 문구로 정리한다. */
    private Indicator toPublicIndicator(AnalysisIndicator indicator) {
        return new Indicator(
                indicator.getType(),
                normalizeIndicatorDescription(indicator.getDescription())
        );
    }

    private String normalizeIndicatorDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.startsWith(MATCHED_RULE_PREFIX)) {
            return description.substring(MATCHED_RULE_PREFIX.length()).trim();
        }
        return switch (description) {
            case "Malicious URL detected" -> "위험한 링크가 확인됐습니다.";
            case "Shortened URL destination was traced" -> "단축 링크의 최종 목적지를 확인했습니다.";
            case "Malicious domain pattern detected" -> "위험한 링크 형식이 확인됐습니다.";
            default -> description;
        };
    }

    /** 신규 raw token과 과거 영문 접두사 형식을 모두 failedTracks 계약으로 복원한다. */
    private String normalizeFailedTrack(String description) {
        String normalized = description.startsWith(FAILED_TRACK_PREFIX)
                ? description.substring(FAILED_TRACK_PREFIX.length())
                : description;
        return normalized.trim();
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

    private Integer rawScoreOrLegacyValue(
            Integer rawScore,
            Integer legacyValue
    ) {
        return rawScore != null ? rawScore : legacyValue;
    }
}
