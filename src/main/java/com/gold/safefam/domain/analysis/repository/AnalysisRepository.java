package com.gold.safefam.domain.analysis.repository;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 문자 분석 Aggregate의 저장과 인증 사용자별 조회를 담당한다.
 * 사용자 ID를 모든 이력 조회 조건에 포함해 다른 사용자의 분석이 노출되지 않게 한다.
 */
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    /** 같은 사용자가 같은 메시지를 다시 분석할 때 기존 결과를 찾는다. */
    Optional<Analysis> findByUserIdAndClientMessageId(Long userId, String clientMessageId);

    /** 상세·삭제·피드백 처리 전에 분석 이력의 소유권을 함께 확인한다. */
    Optional<Analysis> findByIdAndUserId(Long id, Long userId);

    /** 사용자 소유 이력을 위험 등급·피싱 유형·날짜 범위로 필터링한다. */
    @Query("""
        SELECT analysis
        FROM Analysis analysis
        WHERE analysis.userId = :userId
          AND (:riskLevel IS NULL OR analysis.riskLevel = :riskLevel)
          AND (:category IS NULL OR analysis.category = :category)
          AND analysis.analyzedAt >= :fromAt
          AND analysis.analyzedAt < :toExclusive
        """)
    Page<Analysis> search(
            @Param("userId") Long userId,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("category") PhishingCategory category,
            @Param("fromAt") OffsetDateTime fromAt,
            @Param("toExclusive") OffsetDateTime toExclusive,
            Pageable pageable
    );

    /** 사용자의 전체 분석 결과를 위험 등급별로 집계한다. */
    @Query("""
            SELECT analysis.riskLevel AS riskLevel, COUNT(analysis) AS count
            FROM Analysis analysis
            WHERE analysis.userId = :userId
            GROUP BY analysis.riskLevel
            """)
    List<RiskCount> countByRiskLevel(@Param("userId") Long userId);

    /** 사용자의 지정 시점 이후 분석 결과를 위험 등급별로 집계한다. */
    @Query("""
            SELECT analysis.riskLevel AS riskLevel, COUNT(analysis) AS count
            FROM Analysis analysis
            WHERE analysis.userId = :userId
              AND analysis.analyzedAt >= :fromAt
            GROUP BY analysis.riskLevel
            """)
    List<RiskCount> countByRiskLevelSince(
            @Param("userId") Long userId,
            @Param("fromAt") OffsetDateTime fromAt
    );

    /** 사용자의 전체 분석 결과를 피싱 유형별로 집계한다. */
    @Query("""
            SELECT analysis.category AS category, COUNT(analysis) AS count
            FROM Analysis analysis
            WHERE analysis.userId = :userId
            GROUP BY analysis.category
            """)
    List<CategoryCount> countByCategory(@Param("userId") Long userId);

    /** 사용자의 지정 시점 이후 분석 결과를 피싱 유형별로 집계한다. */
    @Query("""
            SELECT analysis.category AS category, COUNT(analysis) AS count
            FROM Analysis analysis
            WHERE analysis.userId = :userId
              AND analysis.analyzedAt >= :fromAt
            GROUP BY analysis.category
            """)
    List<CategoryCount> countByCategorySince(
            @Param("userId") Long userId,
            @Param("fromAt") OffsetDateTime fromAt
    );

    /** 지정 월의 중·고위험 탐지를 사용자 식별자 없이 전체 표본 수로 집계한다. */
    @Query("""
            SELECT COUNT(analysis)
            FROM Analysis analysis
            WHERE analysis.analyzedAt >= :fromAt
              AND analysis.analyzedAt < :toExclusive
              AND analysis.riskLevel IN :riskLevels
            """)
    long countTrendSamples(
            @Param("fromAt") OffsetDateTime fromAt,
            @Param("toExclusive") OffsetDateTime toExclusive,
            @Param("riskLevels") List<RiskLevel> riskLevels
    );

    /** 지정 월의 중·고위험 탐지를 피싱 유형별로 내림차순 집계한다. */
    @Query("""
            SELECT analysis.category AS category, COUNT(analysis) AS count
            FROM Analysis analysis
            WHERE analysis.analyzedAt >= :fromAt
              AND analysis.analyzedAt < :toExclusive
              AND analysis.riskLevel IN :riskLevels
              AND analysis.category <> :excludedCategory
            GROUP BY analysis.category
            ORDER BY COUNT(analysis) DESC, analysis.category ASC
            """)
    List<CategoryCount> findTopTrendCategories(
            @Param("fromAt") OffsetDateTime fromAt,
            @Param("toExclusive") OffsetDateTime toExclusive,
            @Param("riskLevels") List<RiskLevel> riskLevels,
            @Param("excludedCategory") PhishingCategory excludedCategory,
            Pageable pageable
    );

    /** 지정 월의 중·고위험 탐지에서 개인정보 없는 표준 위험 키워드를 집계한다. */
    @Query("""
            SELECT keyword.keyword AS keyword, COUNT(keyword) AS count
            FROM Analysis analysis
            JOIN analysis.keywords keyword
            WHERE analysis.analyzedAt >= :fromAt
              AND analysis.analyzedAt < :toExclusive
              AND analysis.riskLevel IN :riskLevels
            GROUP BY keyword.keyword
            ORDER BY COUNT(keyword) DESC, keyword.keyword ASC
            """)
    List<KeywordCount> findTopTrendKeywords(
            @Param("fromAt") OffsetDateTime fromAt,
            @Param("toExclusive") OffsetDateTime toExclusive,
            @Param("riskLevels") List<RiskLevel> riskLevels,
            Pageable pageable
    );

    /** 위험 등급별 집계 결과를 받는 조회 전용 프로젝션이다. */
    interface RiskCount {
        RiskLevel getRiskLevel();

        long getCount();
    }

    /** 피싱 유형별 집계 결과를 받는 조회 전용 프로젝션이다. */
    interface CategoryCount {
        PhishingCategory getCategory();

        long getCount();
    }

    /** 표준 위험 키워드별 집계 결과를 받는 조회 전용 프로젝션이다. */
    interface KeywordCount {
        String getKeyword();

        long getCount();
    }

}
