package com.gold.safefam.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "분석 결과 기반 멀티턴 챗봇 요청")
public record ChatRequest(
        @NotNull
        @Positive
        @Schema(description = "상담 컨텍스트로 사용할 본인 소유 분석 ID", example = "101")
        Long analysisId,

        @NotEmpty
        @Size(max = 50)
        List<@Valid ChatMessage> messages
) {
    public ChatRequest {
        messages = messages == null ? null : List.copyOf(messages);
    }
}
