package com.gold.safefam.domain.whitelist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 화이트리스트에 등록할 발신자와 화면 표시용 라벨을 전달한다. */
@Schema(description = "화이트리스트 발신자 등록 요청")
public record WhitelistRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(example = "15881234")
        String sender,

        @Size(max = 50)
        @Schema(example = "주거래 은행")
        String label
) {
}
