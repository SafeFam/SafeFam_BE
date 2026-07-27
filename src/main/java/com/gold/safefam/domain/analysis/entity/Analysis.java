package com.gold.safefam.domain.analysis.entity;

import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 문자 원문을 제외한 분석 결과를 저장하는 Aggregate Root.
 * 하위 탐지 근거와 URL 위험 결과의 생명주기를 함께 관리한다.
 */
@Getter
@Entity
@Table(name = "detection_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "client_message_id", length = 100)
    private String clientMessageId;

    @Column(length = 100)
    private String sender;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "content_preview", nullable = false, length = 200)
    private String contentPreview;

    @Enumerated(EnumType.STRING)
    @Column(name = "sms_type", length = 30)
    private PhishingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 30)
    private AnalysisStatus status;

    @Column(name = "llm_score")
    private Integer llmScore;

    @Column(name = "url_score")
    private Integer urlScore;

    @Column(name = "pattern_score")
    private Integer patternScore;

    @Column(name = "total_score")
    private Integer totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private RiskLevel riskLevel;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "detected_at")
    private OffsetDateTime analyzedAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<AnalysisIndicator> indicators = new ArrayList<>();

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<AnalysisUrlRisk> urlRisks = new ArrayList<>();

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<AnalysisKeyword> keywords = new ArrayList<>();

    public Analysis(
            Long userId,
            String clientMessageId,
            String sender,
            String contentHash,
            String contentPreview,
            PhishingCategory category,
            AnalysisSource source,
            int urlScore,
            int patternScore,
            int totalScore,
            RiskLevel riskLevel,
            String explanation,
            OffsetDateTime receivedAt,
            OffsetDateTime analyzedAt
    ) {
        this.userId = userId;
        this.clientMessageId = clientMessageId;
        this.sender = sender;
        this.contentHash = contentHash;
        this.contentPreview = contentPreview;
        this.category = category;
        this.source = source;
        this.status = AnalysisStatus.COMPLETED;
        this.llmScore = 0;
        this.urlScore = urlScore;
        this.patternScore = patternScore;
        this.totalScore = totalScore;
        this.riskLevel = riskLevel;
        this.explanation = explanation;
        this.receivedAt = receivedAt;
        this.analyzedAt = analyzedAt;
    }

    public static Analysis pending(
            Long userId,
            String clientMessageId,
            String sender,
            String contentHash,
            String contentPreview,
            AnalysisSource source,
            OffsetDateTime receivedAt
    ) {
        Analysis analysis = new Analysis();

        analysis.userId = userId;
        analysis.clientMessageId = clientMessageId;
        analysis.sender = sender;
        analysis.contentHash = contentHash;
        analysis.contentPreview = contentPreview;
        analysis.source = source;
        analysis.receivedAt = receivedAt;
        analysis.status = AnalysisStatus.PENDING;

        analysis.category = null;
        analysis.llmScore = null;
        analysis.urlScore = null;
        analysis.patternScore = null;
        analysis.totalScore = null;
        analysis.riskLevel = null;
        analysis.explanation = null;
        analysis.analyzedAt = null;

        return analysis;
    }

    /** 분석 근거를 Aggregate에 연결해 Analysis 저장 트랜잭션에 함께 참여시킨다. */
    public void addIndicator(AnalysisIndicator indicator) {
        indicator.attachTo(this);
        indicators.add(indicator);
    }

    /** URL 위험 결과를 Aggregate에 연결해 Analysis 저장 트랜잭션에 함께 참여시킨다. */
    public void addUrlRisk(AnalysisUrlRisk urlRisk) {
        urlRisk.attachTo(this);
        urlRisks.add(urlRisk);
    }

    /** 개인정보가 없는 표준 위험 키워드를 Aggregate에 연결한다. */
    public void addKeyword(AnalysisKeyword keyword) {
        keyword.attachTo(this);
        keywords.add(keyword);
    }
}
