package com.gold.safefam.domain.family.dto;

import com.gold.safefam.domain.family.entity.FamilyLink;
import com.gold.safefam.domain.family.enums.FamilyLinkStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "가족 연결 정보 응답")
public record FamilyMemberResponse(
        Long linkId,
        Long wardId,
        String wardPhone,
        FamilyLinkStatus status,
        OffsetDateTime linkedAt
) {
    public static FamilyMemberResponse from(FamilyLink link) {
        return new FamilyMemberResponse(
                link.getId(),
                link.getWard() != null ? link.getWard().getId() : null,
                link.getWard() != null ? link.getWard().getPhoneNumber() : null,
                link.getStatus(),
                link.getLinkedAt()
        );
    }
}