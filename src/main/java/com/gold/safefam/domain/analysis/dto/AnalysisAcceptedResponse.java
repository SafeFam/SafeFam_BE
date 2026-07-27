package com.gold.safefam.domain.analysis.dto;
import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비동기 문자 분석 접수 결과")
public record AnalysisAcceptedResponse(

        @Schema(description = "분석 요청 식별자", example = "101")
        Long analysisId,

        @Schema(description = "현재 분석 처리 상태", example = "PENDING")
        AnalysisStatus status
) {
}
