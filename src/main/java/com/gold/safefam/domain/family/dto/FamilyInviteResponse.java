package com.gold.safefam.domain.family.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "초대 코드 / QR 토큰 생성 응답")
public record FamilyInviteResponse(
        @Schema(description = "초대 코드", example = "A1B2C3D4")
        String inviteCode,

        @Schema(description = "QR 토큰", example = "550e8400-e29b-41d4-a716-446655440000")
        String qrToken,

        @Schema(description = "만료 시각 (ISO-8601)")
        OffsetDateTime expiresAt
) {}