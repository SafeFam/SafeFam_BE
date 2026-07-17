package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 로그인 요청")
public record KakaoLoginRequest(
        @NotBlank
        @Schema(description = "Flutter에서 받은 카카오 액세스 토큰")
        String kakaoAccessToken
) {
}