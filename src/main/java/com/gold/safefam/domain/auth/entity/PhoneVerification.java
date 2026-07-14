package com.gold.safefam.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 회원가입 전 휴대폰 본인인증 상태를 저장하는 Entity
 * 인증번호 원문 대신 BCrypt 해시만 보관하고 번호별 최신 요청 하나만 유지함
 */
@Getter
@Entity
@Table(name = "phone_verifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "code_hash", nullable = false, length = 60)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public PhoneVerification(
            String phoneNumber,
            String codeHash,
            Instant expiresAt,
            Instant lastSentAt
    ) {
        this.phoneNumber = phoneNumber;
        renew(codeHash, expiresAt, lastSentAt);
    }

    /** 인증번호 재발송 시 기존 인증 상태와 실패 횟수를 새 요청 기준으로 초기화함 */
    public void renew(String codeHash, Instant expiresAt, Instant sentAt) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.lastSentAt = sentAt;
        this.failedAttempts = 0;
        this.verifiedAt = null;
    }

    /** 마지막 발송 시각을 기준으로 재전송 제한 시간이 지났는지 확인함 */
    public boolean canResend(Instant now, long cooldownMillis) {
        return !now.isBefore(lastSentAt.plusMillis(cooldownMillis));
    }

    /** 현재 시각이 인증번호 만료 시각에 도달했는지 확인함 */
    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** 인증번호 검증이 완료됐는지 확인함 */
    public boolean isVerified() {
        return verifiedAt != null;
    }

    /** 무차별 대입을 막기 위해 허용된 실패 횟수를 모두 사용했는지 확인함 */
    public boolean hasExceededAttempts(int maxAttempts) {
        return failedAttempts >= maxAttempts;
    }

    /** 잘못된 인증번호 입력 횟수 기록 */
    public void recordFailure() {
        failedAttempts++;
    }

    /** 인증 성공 시각을 기록해 회원가입에서 사용할 수 있는 상태로 변경함 */
    public void markVerified(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
