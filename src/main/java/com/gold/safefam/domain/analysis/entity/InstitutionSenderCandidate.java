package com.gold.safefam.domain.analysis.entity;

import com.gold.safefam.domain.analysis.enums.SenderCandidateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** 실제 분석 요청에서 관찰된 기관별 발신번호 후보. */
@Getter
@Entity
@Table(
        name = "institution_sender_candidates",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_institution_sender_candidate",
                columnNames = {"institution", "normalized_sender"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstitutionSenderCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String institution;

    @Column(name = "normalized_sender", nullable = false, length = 20)
    private String normalizedSender;

    @Column(name = "observation_count", nullable = false)
    private int observationCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SenderCandidateStatus status;

    @Column(name = "first_seen_at", nullable = false)
    private OffsetDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "last_analysis_id", nullable = false)
    private Long lastAnalysisId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    public InstitutionSenderCandidate(
            String institution,
            String normalizedSender,
            Long analysisId,
            OffsetDateTime observedAt,
            int reviewThreshold
    ) {
        this.institution = institution;
        this.normalizedSender = normalizedSender;
        this.observationCount = 1;
        this.status = reviewThreshold <= 1
                ? SenderCandidateStatus.REVIEW_REQUIRED
                : SenderCandidateStatus.COLLECTING;
        this.firstSeenAt = observedAt;
        this.lastSeenAt = observedAt;
        this.lastAnalysisId = analysisId;
    }

    /** 같은 기관·번호 조합의 추가 관찰을 누적한다. */
    public void observe(
            Long analysisId,
            OffsetDateTime observedAt,
            int reviewThreshold
    ) {
        observationCount++;
        lastSeenAt = observedAt;
        lastAnalysisId = analysisId;

        if (status == SenderCandidateStatus.COLLECTING
                && observationCount >= reviewThreshold) {
            status = SenderCandidateStatus.REVIEW_REQUIRED;
        }
    }

    /** 공식 채널 검증을 마친 후보를 승인한다. */
    public void approve(OffsetDateTime reviewedAt) {
        status = SenderCandidateStatus.APPROVED;
        this.reviewedAt = reviewedAt;
    }

    /** 공식 번호가 아닌 것으로 확인된 후보를 거절한다. */
    public void reject(OffsetDateTime reviewedAt) {
        status = SenderCandidateStatus.REJECTED;
        this.reviewedAt = reviewedAt;
    }
}
