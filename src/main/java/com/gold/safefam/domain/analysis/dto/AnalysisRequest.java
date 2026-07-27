package com.gold.safefam.domain.analysis.dto;

import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Schema(description = "문자 분석 요청")
public record AnalysisRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "동일 문자 중복 분석 방지를 위한 클라이언트 식별자", example = "sms-20260628-001")
        String clientMessageId,

        @Size(max = 100)
        @Schema(example = "15881234")
        String sender,

        @NotBlank
        @Size(max = 5000)
        @Schema(example = "[국민OO은행] 대출 승인 완료. 아래 링크에서 본인 인증을 진행하세요.")
        String content,

        @NotNull
        OffsetDateTime receivedAt,

        @NotNull
        @Schema(example = "AUTO")
        AnalysisSource source
) {
}
