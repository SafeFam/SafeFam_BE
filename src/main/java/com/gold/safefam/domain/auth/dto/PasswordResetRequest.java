package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 재설정 요청")
public record PasswordResetRequest(
        @NotBlank
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$")
        @Schema(example = "010-1234-5678")
        String phoneNumber,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,64}$",
                message = "비밀번호는 영문과 숫자를 포함한 8자 이상 64자 이하여야 합니다."
        )
        @Schema(example = "newpass12")
        String newPassword
) {
}