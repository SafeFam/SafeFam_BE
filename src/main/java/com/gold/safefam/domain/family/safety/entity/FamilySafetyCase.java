package com.gold.safefam.domain.family.safety.entity;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.family.safety.enums.FamilySafetyStatus;
import com.gold.safefam.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * HIGH 위험 분석 한 건에 대한 가족 공동 대응 상태를 영속화하는 Aggregate Root다.
 * 마스킹된 위험 요약, 알림 일정, 통화 기록과 최종 처리자를 함께 관리하며
 * 여러 보호자가 동일한 대응 건을 공유하도록 analysis_id를 고유 키로 사용한다.
 */
@Getter
@Entity
@Table(name = "family_safety_cases")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilySafetyCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false, unique = true)
    private Analysis analysis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ward_id", nullable = false)
    private User ward;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FamilySafetyStatus status;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "suspected_institution", nullable = false, length = 100)
    private String suspectedInstitution;

    @Column(name = "risky_action", nullable = false, length = 500)
    private String riskyAction;

    @Column(name = "masked_message_preview", nullable = false, length = 200)
    private String maskedMessagePreview;

    @Column(name = "masked_risk_summary", nullable = false, length = 1000)
    private String maskedRiskSummary;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "first_notified_at")
    private OffsetDateTime firstNotifiedAt;

    @Column(name = "last_notified_at")
    private OffsetDateTime lastNotifiedAt;

    @Column(name = "next_reminder_at")
    private OffsetDateTime nextReminderAt;

    @Column(name = "reminder_count", nullable = false)
    private int reminderCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "called_by")
    private User calledBy;

    @Column(name = "called_at")
    private OffsetDateTime calledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    /** HIGH 분석 결과의 안전한 스냅샷과 최초 재알림 시각으로 새 대응 건을 생성한다. */
    public static FamilySafetyCase create(
            Analysis analysis,
            User ward,
            String suspectedInstitution,
            String riskyAction,
            String maskedMessagePreview,
            String maskedRiskSummary,
            OffsetDateTime now,
            OffsetDateTime nextReminderAt
    ) {
        if (analysis.getTotalScore() == null) {
            throw new IllegalArgumentException("Risk score is required for a family safety case");
        }

        FamilySafetyCase safetyCase = new FamilySafetyCase();
        safetyCase.analysis = analysis;
        safetyCase.ward = ward;
        safetyCase.status = FamilySafetyStatus.PENDING;
        safetyCase.riskScore = analysis.getTotalScore();
        safetyCase.suspectedInstitution = suspectedInstitution;
        safetyCase.riskyAction = riskyAction;
        safetyCase.maskedMessagePreview = maskedMessagePreview;
        safetyCase.maskedRiskSummary = maskedRiskSummary;
        safetyCase.detectedAt = analysis.getAnalyzedAt() != null
                ? analysis.getAnalyzedAt()
                : now;
        safetyCase.nextReminderAt = nextReminderAt;
        safetyCase.createdAt = now;
        safetyCase.updatedAt = now;
        return safetyCase;
    }

    /** 한 대 이상의 보호자 기기에 FCM이 성공적으로 전달된 시각을 기록한다. */
    public void recordNotificationDelivered(OffsetDateTime deliveredAt) {
        if (firstNotifiedAt == null) {
            firstNotifiedAt = deliveredAt;
        }
        lastNotifiedAt = deliveredAt;
        updatedAt = deliveredAt;
    }

    /** 스케줄러가 미확인 건을 선점하고 다음 재알림 시각을 예약한다. */
    public void claimReminder(OffsetDateTime now, OffsetDateTime nextReminderAt) {
        if (status.isResolved()) {
            return;
        }
        reminderCount++;
        this.nextReminderAt = nextReminderAt;
        updatedAt = now;
    }

    /** 보호자의 전화 시도를 최초 한 번 기록하고 CONTACTING 상태로 전환한다. */
    public void startCall(User guardian, OffsetDateTime now) {
        if (status.isResolved()) {
            throw new IllegalStateException("Resolved safety case cannot start a call");
        }
        status = FamilySafetyStatus.CONTACTING;
        if (calledAt == null) {
            calledBy = guardian;
            calledAt = now;
        }
        updatedAt = now;
    }

    /** 안전 확인 또는 송금 발생 결과를 확정하고 이후 재알림을 중단한다. */
    public void resolve(FamilySafetyStatus resolution, User guardian, OffsetDateTime now) {
        if (!resolution.isResolved()) {
            throw new IllegalArgumentException("Only a resolved status is allowed");
        }
        if (status.isResolved()) {
            if (status == resolution) {
                return;
            }
            throw new IllegalStateException("Safety case has already been resolved");
        }

        status = resolution;
        handledBy = guardian;
        resolvedAt = now;
        nextReminderAt = null;
        updatedAt = now;
    }
}
