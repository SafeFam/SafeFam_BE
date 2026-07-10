package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청")
public record ReissueRequest(
        @NotBlank
        @Schema(example = "refresh-token-value")
        String refreshToken
) {
}
