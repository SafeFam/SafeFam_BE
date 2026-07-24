package com.gold.safefam.domain.family.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "초대 코드로 연결 수락 요청")
public record FamilyLinkByCodeRequest(
        @NotBlank
        @Size(min = 6, max = 6)
        @Pattern(regexp = "\\d{6}", message = "초대 코드는 숫자 6자리여야 합니다.")
        @Schema(description = "초대 코드", example = "123456")
        String inviteCode
) {}