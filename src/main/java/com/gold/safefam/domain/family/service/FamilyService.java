package com.gold.safefam.domain.family.service;

import com.gold.safefam.domain.analysis.dto.AnalysisListItemResponse;
import com.gold.safefam.domain.analysis.service.AnalysisService;
import com.gold.safefam.domain.family.dto.FamilyInviteResponse;
import com.gold.safefam.domain.family.dto.FamilyMemberResponse;
import com.gold.safefam.domain.family.entity.FamilyLink;
import com.gold.safefam.domain.family.enums.FamilyLinkStatus;
import com.gold.safefam.domain.family.repository.FamilyLinkRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import com.gold.safefam.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyService {

    private static final int INVITE_EXPIRE_MINUTES = 10;
    private static final int INVITE_CODE_LENGTH = 6;

    private final FamilyLinkRepository familyLinkRepository;
    private final UserRepository userRepository;
    private final AnalysisService analysisService;

    // ─── 초대 생성 (보호자) ────────────────────────────────────

    @Transactional
    public FamilyInviteResponse createInvite(Long protectorId) {
        User protector = findUser(protectorId);

        String inviteCode = generateInviteCode();
        String qrToken = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(INVITE_EXPIRE_MINUTES);

        FamilyLink link = FamilyLink.createInvite(protector, inviteCode, qrToken, expiresAt);
        familyLinkRepository.save(link);

        return new FamilyInviteResponse(inviteCode, qrToken, expiresAt);
    }

    // ─── 초대 수락 — 코드 (피보호자) ──────────────────────────

    @Transactional
    public void linkByCode(Long wardId, String inviteCode) {
        FamilyLink link = familyLinkRepository
                .findByInviteCodeAndStatus(inviteCode, FamilyLinkStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_INVITE_NOT_FOUND));

        validateAndAccept(link, wardId);
    }

    // ─── 초대 수락 — QR (피보호자) ────────────────────────────

    @Transactional
    public void linkByQr(Long wardId, String qrToken) {
        FamilyLink link = familyLinkRepository
                .findByQrTokenAndStatus(qrToken, FamilyLinkStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_INVITE_NOT_FOUND));

        validateAndAccept(link, wardId);
    }

    // ─── 연결 해제 ─────────────────────────────────────────────

    @Transactional
    public void revoke(Long requesterId, Long linkId) {
        FamilyLink link = familyLinkRepository.findById(linkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_LINK_NOT_FOUND));

        boolean isParty = link.getProtector().getId().equals(requesterId)
                || (link.getWard() != null && link.getWard().getId().equals(requesterId));

        if (!isParty) {
            throw new BusinessException(ErrorCode.FAMILY_LINK_FORBIDDEN);
        }

        link.revoke();
    }

    // ─── 가족 목록 조회 (보호자·피보호자 공통) ──────────────────

    public List<FamilyMemberResponse> getMembers(Long userId) {
        return familyLinkRepository
                .findAllByParticipantIdAndStatus(userId, FamilyLinkStatus.ACTIVE)
                .stream()
                .map(link -> FamilyMemberResponse.from(link, userId))
                .toList();
    }

    // ─── 관계 검증 (탐지 이력 조회 권한) ──────────────────────

    public void validateGuardianAccess(Long protectorId, Long wardId) {
        boolean linked = familyLinkRepository
                .existsByProtectorIdAndWardIdAndStatus(protectorId, wardId, FamilyLinkStatus.ACTIVE);
        if (!linked) {
            throw new BusinessException(ErrorCode.FAMILY_LINK_FORBIDDEN);
        }
    }

    // ─── 피보호자 탐지 이력 조회 (보호자) ─────────────────────────

    public PageResponse<AnalysisListItemResponse> getWardLogs(Long protectorId, Long wardId, int page, int size) {
        validateGuardianAccess(protectorId, wardId);
        return analysisService.getAnalyses(wardId, page, size, null, null, null, null);
    }

    // ─── 관계 설정 (보호자) ────────────────────────────────────────

    @Transactional
    public void updateRelationship(Long protectorId, Long linkId, String relationship) {
        FamilyLink link = familyLinkRepository.findById(linkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_LINK_NOT_FOUND));

        if (!link.getProtector().getId().equals(protectorId)) {
            throw new BusinessException(ErrorCode.FAMILY_LINK_FORBIDDEN);
        }

        link.updateRelationship(relationship);
    }

    // ─── private ───────────────────────────────────────────────

    private void validateAndAccept(FamilyLink link, Long wardId) {
        if (link.isExpired()) {
            throw new BusinessException(ErrorCode.FAMILY_INVITE_EXPIRED);
        }
        if (link.getProtector().getId().equals(wardId)) {
            throw new BusinessException(ErrorCode.FAMILY_SELF_LINK);
        }

        User ward = findUser(wardId);
        link.accept(ward);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String generateInviteCode() {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
}
