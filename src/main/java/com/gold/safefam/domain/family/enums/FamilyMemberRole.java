package com.gold.safefam.domain.family.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가족 연결에서 조회된 상대방의 역할")
public enum FamilyMemberRole {
    PROTECTOR,
    WARD
}
