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

/** 사용자가 이해할 수 있는 표현으로 정리된 분석 근거 카드. */
@Getter
@Entity
@Table(name = "detection_evidence_cards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisEvidenceCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detection_log_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    public AnalysisEvidenceCard(
            String category,
            String title,
            String description
    ) {
        this.category = category;
        this.title = title;
        this.description = description;
    }

    void attachTo(Analysis analysis) {
        this.analysis = analysis;
    }
}
