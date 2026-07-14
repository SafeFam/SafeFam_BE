package com.gold.safefam.domain.auth.service;

import com.gold.safefam.domain.auth.entity.PhoneVerification;
import com.gold.safefam.domain.auth.repository.PhoneVerificationRepository;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import com.gold.safefam.global.sms.SmsDeliveryException;
import com.gold.safefam.global.sms.SmsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * 회원가입 전 휴대폰 인증번호의 발송, 검증, 소비를 담당함
 * SMS 발송 자체는 SmsSender에 위임하고 인증 정책과 DB 상태만 관리함
 */
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;

    @Value("${phone-verification.expiration:180000}")
    private long expirationMillis;

    @Value("${phone-verification.resend-cooldown:60000}")
    private long resendCooldownMillis;

    @Value("${phone-verification.max-attempts:5}")
    private int maxAttempts;

    /**
     * 인증번호를 새로 생성해 SMS로 발송하고 해시만 저장함
     * 이미 가입된 번호와 재전송 제한 시간 내 요청은 발송 전에 차단함
     */
    @Transactional
    public void sendCode(String requestedPhoneNumber) {
        String phoneNumber = normalize(requestedPhoneNumber);
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        Instant now = Instant.now();
        PhoneVerification verification = phoneVerificationRepository
                .findByPhoneNumber(phoneNumber)
                .orElse(null);
        if (verification != null && !verification.canResend(now, resendCooldownMillis)) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_RESEND_TOO_SOON);
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        // DB가 노출되어도 인증번호 원문을 바로 확인할 수 없도록 BCrypt 해시만 저장함
        String codeHash = passwordEncoder.encode(code);
        Instant expiresAt = now.plusMillis(expirationMillis);
        if (verification == null) {
            verification = new PhoneVerification(phoneNumber, codeHash, expiresAt, now);
        } else {
            verification.renew(codeHash, expiresAt, now);
        }

        try {
            smsSender.sendVerificationCode(phoneNumber, code);
        } catch (SmsDeliveryException exception) {
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
        phoneVerificationRepository.save(verification);
    }

    /**
     * 입력된 인증번호의 만료·실패 횟수·해시 일치 여부를 검증함
     * noRollbackFor를 사용해 잘못된 번호로 예외가 발생해도 실패 횟수는 DB에 반영함
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyCode(String requestedPhoneNumber, String code) {
        String phoneNumber = normalize(requestedPhoneNumber);
        PhoneVerification verification = phoneVerificationRepository
                .findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_VERIFICATION_NOT_FOUND));

        if (verification.isVerified()) {
            return;
        }
        if (verification.isExpired(Instant.now())) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_EXPIRED);
        }
        if (verification.hasExceededAttempts(maxAttempts)) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_ATTEMPTS_EXCEEDED);
        }
        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.recordFailure();
            throw new BusinessException(ErrorCode.INVALID_PHONE_VERIFICATION_CODE);
        }

        verification.markVerified(Instant.now());
    }

    /**
     * 회원가입에서 인증 완료 상태를 한 번만 사용하고 삭제함
     * 삭제는 회원 저장과 같은 트랜잭션에 참여하므로 가입 실패 시 함께 롤백됨
     */
    @Transactional
    public String consumeVerified(String requestedPhoneNumber) {
        String phoneNumber = normalize(requestedPhoneNumber);
        PhoneVerification verification = phoneVerificationRepository
                .findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_VERIFICATION_REQUIRED));
        if (!verification.isVerified() || verification.isExpired(Instant.now())) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_REQUIRED);
        }
        phoneVerificationRepository.delete(verification);
        return phoneNumber;
    }

    /** 하이픈이 포함된 입력을 DB 비교에 사용하는 숫자 전용 형식으로 통일함 */
    private String normalize(String phoneNumber) {
        return phoneNumber.replace("-", "");
    }
}
