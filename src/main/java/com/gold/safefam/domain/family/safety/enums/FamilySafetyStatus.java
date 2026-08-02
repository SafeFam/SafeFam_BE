package com.gold.safefam.domain.family.safety.enums;

/**
 * HIGH 위험 문자에 대한 가족 공동 대응 진행 상태를 나타낸다.
 * PENDING과 CONTACTING은 재알림 대상이며,
 * SAFE_CONFIRMED와 TRANSFERRED는 보호자가 결과를 확정한 종료 상태다.
 */
public enum FamilySafetyStatus {
    PENDING,
    CONTACTING,
    SAFE_CONFIRMED,
    TRANSFERRED;

    /** 종료 상태인지 판단해 상태 전이와 재알림 중단 조건에 사용한다. */
    public boolean isResolved() {
        return this == SAFE_CONFIRMED || this == TRANSFERRED;
    }
}
