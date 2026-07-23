package com.gold.safefam.domain.auth.repository;

import com.gold.safefam.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Refresh Token DB 조회, 저장을 담당함
 * Spring Data JPA가 메서드 이름을 기반으로 쿼리 생성
 */

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByExpiresAtBefore(Instant now);
}
