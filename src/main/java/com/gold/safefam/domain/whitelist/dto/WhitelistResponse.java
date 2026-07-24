package com.gold.safefam.domain.whitelist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/** 등록된 화이트리스트 항목의 식별자·정규화 발신자·라벨·등록 시각을 반환한다. */
@Schema(description = "화이트리스트 발신자")
public record WhitelistResponse(
        Long whitelistId,
        String sender,
        String label,
        OffsetDateTime createdAt
) {
}
