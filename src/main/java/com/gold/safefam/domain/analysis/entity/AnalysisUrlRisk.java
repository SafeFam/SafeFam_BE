package com.gold.safefam.domain.analysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 외부 평판이 아닌 URL 형태 기반 위험 결과 한 건을 저장하는 Analysis 하위 엔티티. */
@Getter
@Entity
@Table(name = "detection_url_risks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisUrlRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detection_log_id", nullable = false)
    private Analysis analysis;

    @Column(name = "original_url", nullable = false, length = 500)
    private String originalUrl;

    @Column(nullable = false)
    private boolean shortened;

    @Column(nullable = false)
    private boolean suspicious;

    @Column(name = "traced_url", length = 500)
    private String tracedUrl;

    @Column
    private Boolean malicious;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "engine_source", length = 100)
    private String engineSource;

    @Column(name = "error_code", length = 200)
    private String errorCode;

    /**
     * 기존 Spring 규칙 기반 URL 분석 결과를 저장한다.
     *
     * 기존 분석기는 최종 추적 URL과 외부 엔진 상세 정보를 제공하지 않으므로
     * 해당 값은 null로 유지하고, 자체 계산한 shortened/suspicious 값을 보존한다.
     */
    public AnalysisUrlRisk(
            String originalUrl,
            boolean shortened,
            boolean suspicious
    ) {
        this.originalUrl = originalUrl;
        this.shortened = shortened;
        this.suspicious = suspicious;
        this.tracedUrl = null;
        this.malicious = suspicious;
        this.riskScore = null;
        this.engineSource = null;
        this.errorCode = null;
    }

    /**
     * FastAPI가 전달한 URL 분석 결과를 저장한다.
     */
    public AnalysisUrlRisk(
            String originalUrl,
            String tracedUrl,
            Boolean malicious,
            Integer riskScore,
            String engineSource,
            String errorCode
    ) {
        this.originalUrl = originalUrl;
        this.tracedUrl = tracedUrl;
        this.malicious = malicious;
        this.riskScore = riskScore;
        this.engineSource = engineSource;
        this.errorCode = errorCode;

        this.shortened = originalUrl != null
                && tracedUrl != null
                && !originalUrl.equals(tracedUrl);

        this.suspicious = Boolean.TRUE.equals(malicious);

    }

    void attachTo(Analysis analysis) {
        this.analysis = analysis;
    }
}
