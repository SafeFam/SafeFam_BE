package com.gold.safefam.domain.auth.repository;

import com.gold.safefam.domain.auth.entity.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 휴대폰 번호별 인증 상태 조회 및 저장 */
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    Optional<PhoneVerification> findByPhoneNumber(String phoneNumber);
}
