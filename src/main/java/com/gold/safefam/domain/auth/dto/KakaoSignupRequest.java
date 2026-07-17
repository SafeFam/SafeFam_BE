package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "카카오 회원가입 요청")
public record KakaoSignupRequest(
        @NotBlank
        @Schema(description = "카카오 로그인에서 받은 kakaoId")
        String kakaoId,

        @NotBlank
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$")
        @Schema(example = "010-1234-5678")
        String phoneNumber,

        @NotBlank
        @Size(max = 30)
        @Schema(example = "김안전")
        String name
) {
}