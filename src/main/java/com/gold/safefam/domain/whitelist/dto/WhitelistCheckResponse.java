package com.gold.safefam.domain.whitelist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 클라이언트가 분석 API 호출을 생략할지 결정하는 화이트리스트 확인 결과다. */
@Schema(description = "발신자 화이트리스트 확인 결과")
public record WhitelistCheckResponse(
        String sender,

        @Schema(description = "true이면 문자 분석 API 호출을 생략할 수 있음")
        boolean whitelisted
) {
}
