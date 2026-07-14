package com.gold.safefam.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 인증번호를 받을 국내 010 휴대폰 번호 전달 */
@Schema(description = "휴대폰 인증번호 발송 요청")
public record SendPhoneVerificationRequest(
        @NotBlank
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$")
        @Schema(example = "010-1234-5678")
        String phoneNumber
) {
}
