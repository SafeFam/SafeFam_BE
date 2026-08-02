package com.gold.safefam.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "금융 사기 대응 챗봇 응답")
public record ChatResponse(
        @Schema(example = "공식 금융기관 연락처로 거래 내역을 확인하고 계좌 지급정지를 요청하세요.")
        String message
) {
}
