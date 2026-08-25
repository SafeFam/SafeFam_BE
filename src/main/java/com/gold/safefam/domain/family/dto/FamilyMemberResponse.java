package com.gold.safefam.domain.family.dto;

import com.gold.safefam.domain.family.entity.FamilyLink;
import com.gold.safefam.domain.family.enums.FamilyMemberRole;
import com.gold.safefam.domain.family.enums.FamilyLinkStatus;
import com.gold.safefam.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "가족 연결 정보 응답")
public record FamilyMemberResponse(
        Long linkId,
        @Schema(description = "현재 로그인 사용자를 기준으로 한 상대 가족 사용자 ID")
        Long memberId,
        @Schema(description = "현재 로그인 사용자를 기준으로 한 상대 가족 이름")
        String memberName,
        @Schema(description = "현재 로그인 사용자를 기준으로 한 상대 가족 전화번호")
        String memberPhone,
        @Schema(description = "가족 연결에서 상대방의 역할")
        FamilyMemberRole memberRole,
        @Schema(description = "피보호자 ID. 기존 보호자 화면과의 호환을 위해 유지됩니다.")
        Long wardId,
        @Schema(description = "피보호자 이름. 기존 보호자 화면과의 호환을 위해 유지됩니다.")
        String wardName,
        @Schema(description = "피보호자 전화번호. 기존 보호자 화면과의 호환을 위해 유지됩니다.")
        String wardPhone,
        String relationship,
        FamilyLinkStatus status,
        OffsetDateTime linkedAt
) {
    public static FamilyMemberResponse from(FamilyLink link, Long requesterId) {
        boolean requesterIsProtector = link.getProtector().getId().equals(requesterId);
        boolean requesterIsWard = link.getWard() != null && link.getWard().getId().equals(requesterId);

        if (!requesterIsProtector && !requesterIsWard) {
            throw new IllegalArgumentException("Requester is not a participant of the family link");
        }

        User member = requesterIsProtector ? link.getWard() : link.getProtector();
        FamilyMemberRole memberRole = requesterIsProtector
                ? FamilyMemberRole.WARD
                : FamilyMemberRole.PROTECTOR;

        return new FamilyMemberResponse(
                link.getId(),
                member != null ? member.getId() : null,
                member != null ? member.getName() : null,
                member != null ? member.getPhoneNumber() : null,
                memberRole,
                link.getWard() != null ? link.getWard().getId() : null,
                link.getWard() != null ? link.getWard().getName() : null,
                link.getWard() != null ? link.getWard().getPhoneNumber() : null,
                link.getRelationship(),
                link.getStatus(),
                link.getLinkedAt()
        );
    }
}
