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

/** 개인정보가 섞인 임의 단어가 아니라 서버가 허용한 표준 위험 키워드만 저장한다. */
@Getter
@Entity
@Table(name = "detection_keywords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detection_log_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false, length = 50)
    private String keyword;

    /** 사전 정의 목록에서 탐지된 표준 위험 키워드 한 건을 만든다. */
    public AnalysisKeyword(String keyword) {
        this.keyword = keyword;
    }

    /** Analysis Aggregate가 키워드의 생명주기와 외래 키 연결을 관리하도록 소유자를 지정한다. */
    void attachTo(Analysis analysis) {
        this.analysis = analysis;
    }
}
