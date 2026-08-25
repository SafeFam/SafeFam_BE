package com.gold.safefam.domain.family.repository;

import com.gold.safefam.domain.family.entity.FamilyLink;
import com.gold.safefam.domain.family.enums.FamilyLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FamilyLinkRepository extends JpaRepository<FamilyLink, Long> {

    Optional<FamilyLink> findByInviteCodeAndStatus(String inviteCode, FamilyLinkStatus status);

    Optional<FamilyLink> findByQrTokenAndStatus(String qrToken, FamilyLinkStatus status);

    List<FamilyLink> findAllByProtectorIdAndStatus(Long protectorId, FamilyLinkStatus status);

    /** 로그인 사용자가 보호자 또는 피보호자로 참여한 활성 가족 연결을 모두 조회한다. */
    @Query("""
            SELECT fl
            FROM FamilyLink fl
            JOIN FETCH fl.protector
            JOIN FETCH fl.ward
            WHERE fl.status = :status
              AND (fl.protector.id = :userId OR fl.ward.id = :userId)
            ORDER BY fl.linkedAt DESC, fl.id DESC
            """)
    List<FamilyLink> findAllByParticipantIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") FamilyLinkStatus status
    );

    // 보호자가 특정 피보호자의 관계를 가지고 있는지 검증
    boolean existsByProtectorIdAndWardIdAndStatus(Long protectorId, Long wardId, FamilyLinkStatus status);

    // HIGH 탐지 발생 시 피보호자의 보호자 목록 조회
    @Query("SELECT fl FROM FamilyLink fl JOIN FETCH fl.protector WHERE fl.ward.id = :wardId AND fl.status = 'ACTIVE'")
    List<FamilyLink> findActiveByWardId(@Param("wardId") Long wardId);
}
