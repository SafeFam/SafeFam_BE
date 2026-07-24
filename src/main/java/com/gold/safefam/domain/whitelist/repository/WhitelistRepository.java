package com.gold.safefam.domain.whitelist.repository;

import com.gold.safefam.domain.whitelist.entity.WhitelistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 사용자별 신뢰 발신자 등록 상태와 소유권을 조회한다. */
public interface WhitelistRepository extends JpaRepository<WhitelistEntry, Long> {

    /** 사용자가 소유한 항목을 최신 등록순으로 조회한다. */
    List<WhitelistEntry> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    /** 정규화된 발신자의 중복 등록 여부와 프리패스 상태를 조회한다. */
    Optional<WhitelistEntry> findByUserIdAndSender(Long userId, String sender);

    /** 삭제 전에 항목 ID와 사용자 ID를 함께 비교해 소유권을 확인한다. */
    Optional<WhitelistEntry> findByIdAndUserId(Long id, Long userId);
}
