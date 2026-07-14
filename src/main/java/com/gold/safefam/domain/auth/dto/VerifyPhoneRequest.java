package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 휴대폰 번호와 SMS로 받은 6자리 인증번호 전달 */
@Schema(description = "휴대폰 인증번호 검증 요청")
public record VerifyPhoneRequest(
        @NotBlank
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$")
        @Schema(example = "010-1234-5678")
        String phoneNumber,

        @NotBlank
        @Pattern(regexp = "\\d{6}")
        @Schema(example = "123456")
        String code
) {
}
