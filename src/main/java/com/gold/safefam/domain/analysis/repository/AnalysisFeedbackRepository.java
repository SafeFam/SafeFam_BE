package com.gold.safefam.domain.analysis.repository;

import com.gold.safefam.domain.analysis.entity.AnalysisFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 분석 이력별 사용자 피드백을 저장하고 조회한다.
 * 분석당 한 건이라는 DB 제약과 함께 피드백 재전송 시 갱신 흐름을 지원한다.
 */
public interface AnalysisFeedbackRepository extends JpaRepository<AnalysisFeedback, Long> {

    /** 분석 ID로 기존 피드백을 찾아 생성 또는 갱신 여부를 결정한다. */
    Optional<AnalysisFeedback> findByAnalysisId(Long analysisId);
}
