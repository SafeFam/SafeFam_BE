package com.gold.safefam.domain.analysis.dto;

import com.gold.safefam.domain.analysis.enums.FeedbackType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "분석 결과 피드백 요청")
public record AnalysisFeedbackRequest(
        @NotNull
        @Schema(example = "FALSE_POSITIVE")
        FeedbackType type,

        @Size(max = 500)
        @Schema(example = "실제로 사용 중인 은행에서 보낸 정상 문자입니다.")
        String comment
) {
}
