package com.gold.safefam.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 프로필 수정 요청")
public record UpdateUserRequest(
        @NotBlank
        @Size(max = 30)
        @Schema(example = "김세이프")
        String name
) {
}
