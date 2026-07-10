package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Email
        @NotBlank
        @Schema(example = "safe@example.com")
        String email,

        @NotBlank
        @Schema(example = "safePassword123!")
        String password
) {
}
