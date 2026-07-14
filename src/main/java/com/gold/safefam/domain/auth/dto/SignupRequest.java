package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @NotBlank
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$")
        @Schema(example = "010-1234-5678")
        String phoneNumber,

        @Email
        @NotBlank
        @Schema(example = "safe@example.com")
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        @Schema(example = "safePassword123!")
        String password,

        @NotBlank
        @Size(max = 30)
        @Schema(example = "김안전")
        String name
) {
}
