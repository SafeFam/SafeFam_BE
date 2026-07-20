package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "계정 잠금 해제 요청")
public record UnlockRequest(
        @NotBlank
        @Schema(example = "010-1234-5678")
        String phoneNumber
) {
}