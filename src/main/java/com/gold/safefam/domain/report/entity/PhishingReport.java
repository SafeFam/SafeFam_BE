package com.gold.safefam.domain.report.entity;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.report.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

/**
 * 신고자의 식별자는 저장하지 않고 분석 결과의 해시·마스킹 미리보기와 분류만 보관한다.
 * 원본 분석이 삭제되어도 익명 신고 스냅샷은 통계와 탐지 규칙 개선을 위해 유지된다.
 */
@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhishingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detection_log_id", unique = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Analysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "sms_type", nullable = false, length = 30)
    private PhishingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "content_preview", nullable = false, length = 200)
    private String contentPreview;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 분석 원문이나 사용자 ID 없이 보호된 분석 값만 복사해 신고 스냅샷을 만든다. */
    public PhishingReport(Analysis analysis, ReportType type) {
        this.analysis = analysis;
        this.type = type;
        this.category = analysis.getCategory();
        this.riskLevel = analysis.getRiskLevel();
        this.contentHash = analysis.getContentHash();
        this.contentPreview = analysis.getContentPreview();
    }
}
