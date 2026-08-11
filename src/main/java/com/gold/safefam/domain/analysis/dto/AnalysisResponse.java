package com.gold.safefam.domain.analysis.dto;

import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "문자 분석 상세 결과")
public record AnalysisResponse(
        @Schema(example = "101")
        Long analysisId,

        @Schema(
                description = "분석 처리 상태",
                example = "COMPLETED"
        )
        AnalysisStatus status,

        @Schema(minimum = "0", maximum = "100", example = "92")
        Integer riskScore,

        @Schema(example = "HIGH")
        RiskLevel riskLevel,

        @Schema(example = "FINANCIAL_INSTITUTION")
        PhishingCategory category,

        @Schema(example = "금융기관을 사칭하며 외부 링크 접속과 본인 인증을 유도합니다.")
        String explanation,

        @Schema(
                description = "전체 분석 실패 코드",
                example = "PIPELINE_ERROR",
                nullable = true
        )
        String failureCode,

        @Schema(
                description = "부분 분석에서 사용할 수 없었던 분석 계층",
                example = "[\"URL:VIRUSTOTAL\", \"TEXT:LLM\"]"
        )
        List<String> failedTracks,

        ScoreBreakdown scoreBreakdown,

        List<Indicator> indicators,

        List<UrlThreat> urls,

        List<RecommendedAction> recommendedActions,

        OffsetDateTime analyzedAt
) {
    @Schema(description = "탐지 계층별 점수. 현재 규칙 기반 구현에서는 llmScore가 0입니다.")
    public record ScoreBreakdown(
            @Schema(example = "0") Integer llmScore,
            @Schema(example = "100") Integer urlScore,
            @Schema(example = "85") Integer patternScore
    ) {
    }

    @Schema(description = "판단에 사용된 위험 근거")
    public record Indicator(
            @Schema(example = "MALICIOUS_URL") IndicatorType type,
            @Schema(example = "악성 이력이 확인된 URL입니다.") String description
    ) {
    }

    @Schema(description = "문자에 포함된 URL 검사 결과")
    public record UrlThreat(
            @Schema(example = "https://short.example/abc") String originalUrl,
            @Schema(example = "https://malicious.example/login") String resolvedUrl,
            @Schema(description = "URL 형태에서 위험 특성이 발견됐는지 여부", example = "true")
            boolean suspicious
    ) {
    }

    @Schema(description = "사용자가 즉시 수행할 수 있는 대응 방법")
    public record RecommendedAction(
            @Schema(example = "CALL_1332") String type,
            @Schema(example = "금융감독원 1332에 문의하세요.") String label,
            @Schema(example = "1332") String phoneNumber,
            @Schema(example = "https://www.fss.or.kr") String url
    ) {
    }
}
