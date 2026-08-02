package com.gold.safefam.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "멀티턴 대화 메시지")
public record ChatMessage(
        @NotNull
        @Schema(example = "USER")
        ChatRole role,

        @NotBlank
        @Size(max = 2000)
        @Schema(example = "이 문자의 링크를 눌렀는데 지금 무엇을 해야 하나요?")
        String content
) {
}
