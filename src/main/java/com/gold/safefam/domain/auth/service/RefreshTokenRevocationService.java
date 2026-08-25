package com.gold.safefam.domain.auth.service;

import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 실패한 인증 트랜잭션과 무관하게 Refresh Token 폐기를 확정한다. */
@Service
@RequiredArgsConstructor
public class RefreshTokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
