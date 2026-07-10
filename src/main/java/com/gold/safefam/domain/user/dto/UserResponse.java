package com.gold.safefam.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "사용자 정보")
public record UserResponse(
        @Schema(example = "1")
        Long userId,

        @Schema(example = "safe@example.com")
        String email,

        @Schema(example = "김안전")
        String name,

        OffsetDateTime createdAt
) {
}
