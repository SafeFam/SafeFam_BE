package com.gold.safefam.domain.analysis.entity;

import com.gold.safefam.domain.analysis.enums.IndicatorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 분석 점수의 근거 한 건을 저장하는 Analysis 하위 엔티티. */
@Getter
@Entity
@Table(name = "detection_indicators")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detection_log_id", nullable = false)
    private Analysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false, length = 30)
    private IndicatorType type;

    @Column(nullable = false, length = 500)
    private String description;

    public AnalysisIndicator(IndicatorType type, String description) {
        this.type = type;
        this.description = description;
    }

    void attachTo(Analysis analysis) {
        this.analysis = analysis;
    }
}
