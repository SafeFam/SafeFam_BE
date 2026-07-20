package com.gold.safefam.domain.analysis.repository;

import com.gold.safefam.domain.analysis.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 분석 결과 저장과 사용자별 clientMessageId 중복 조회를 담당한다. */
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByUserIdAndClientMessageId(Long userId, String clientMessageId);
}
