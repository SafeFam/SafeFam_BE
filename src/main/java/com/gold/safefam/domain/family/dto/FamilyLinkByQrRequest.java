package com.gold.safefam.domain.family.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "QR 토큰으로 연결 수락 요청")
public record FamilyLinkByQrRequest(
        @NotBlank
        @Size(max = 64)
        @Schema(description = "QR 토큰", example = "550e8400-e29b-41d4-a716-446655440000")
        String qrToken
) {}