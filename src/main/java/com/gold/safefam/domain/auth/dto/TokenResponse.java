package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 토큰 응답")
public record TokenResponse(
        @Schema(example = "Bearer")
        String tokenType,

        @Schema(example = "access-token-value")
        String accessToken,

        @Schema(example = "refresh-token-value")
        String refreshToken,

        @Schema(description = "Access Token 만료까지 남은 초", example = "3600")
        long expiresIn
) {
}
