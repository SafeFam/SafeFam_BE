package com.gold.safefam.domain.report.repository;

import com.gold.safefam.domain.report.entity.PhishingReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 분석 한 건당 하나의 사용자 신고만 유지한다. */
public interface PhishingReportRepository extends JpaRepository<PhishingReport, Long> {

    /** 동일 분석의 재신고 여부를 확인해 신고 API를 멱등하게 처리한다. */
    Optional<PhishingReport> findByAnalysisId(Long analysisId);
}
