package com.gold.safefam.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원 탈퇴 본인 확인 요청")
public record WithdrawalRequest(
        @NotBlank
        @Schema(example = "safePassword123!")
        String password
) {
}
