package com.gold.safefam.domain.family.safety.repository;

import com.gold.safefam.domain.family.safety.entity.FamilySafetyCase;
import com.gold.safefam.domain.family.safety.enums.FamilySafetyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 가족 공동 안전 대응 건의 저장과 보호자별 조회를 담당한다.
 * 상태 변경과 재알림 선점 쿼리에는 비관적 잠금을 적용해
 * 여러 요청 또는 서버가 같은 건을 동시에 처리하는 상황을 방지한다.
 */
public interface FamilySafetyCaseRepository extends JpaRepository<FamilySafetyCase, Long> {

    /** 동일 HIGH 분석에 이미 생성된 공동 대응 건이 있는지 조회한다. */
    Optional<FamilySafetyCase> findByAnalysisId(Long analysisId);

    /** 상세 응답에 필요한 분석, 보호 대상, 통화자와 처리자를 한 번에 조회한다. */
    @Query("""
            SELECT safetyCase
            FROM FamilySafetyCase safetyCase
            JOIN FETCH safetyCase.analysis
            JOIN FETCH safetyCase.ward
            LEFT JOIN FETCH safetyCase.calledBy
            LEFT JOIN FETCH safetyCase.handledBy
            WHERE safetyCase.id = :caseId
            """)
    Optional<FamilySafetyCase> findDetailedById(@Param("caseId") Long caseId);

    /** 전화 또는 완료 처리 중 상태가 동시에 변경되지 않도록 행을 잠가 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT safetyCase
            FROM FamilySafetyCase safetyCase
            JOIN FETCH safetyCase.analysis
            JOIN FETCH safetyCase.ward
            LEFT JOIN FETCH safetyCase.calledBy
            LEFT JOIN FETCH safetyCase.handledBy
            WHERE safetyCase.id = :caseId
            """)
    Optional<FamilySafetyCase> findByIdForUpdate(@Param("caseId") Long caseId);

    /** 현재 보호자가 ACTIVE 관계로 연결된 가족의 대응 건만 페이지로 조회한다. */
    @Query(
            value = """
                    SELECT safetyCase
                    FROM FamilySafetyCase safetyCase
                    JOIN FETCH safetyCase.analysis
                    JOIN FETCH safetyCase.ward
                    LEFT JOIN FETCH safetyCase.calledBy
                    LEFT JOIN FETCH safetyCase.handledBy
                    WHERE (:status IS NULL OR safetyCase.status = :status)
                      AND EXISTS (
                          SELECT link.id
                          FROM FamilyLink link
                          WHERE link.protector.id = :guardianId
                            AND link.ward.id = safetyCase.ward.id
                            AND link.status = 'ACTIVE'
                      )
                    """,
            countQuery = """
                    SELECT COUNT(safetyCase)
                    FROM FamilySafetyCase safetyCase
                    WHERE (:status IS NULL OR safetyCase.status = :status)
                      AND EXISTS (
                          SELECT link.id
                          FROM FamilyLink link
                          WHERE link.protector.id = :guardianId
                            AND link.ward.id = safetyCase.ward.id
                            AND link.status = 'ACTIVE'
                      )
                    """
    )
    Page<FamilySafetyCase> findAccessibleCases(
            @Param("guardianId") Long guardianId,
            @Param("status") FamilySafetyStatus status,
            Pageable pageable
    );

    /** 재알림 시각이 지난 미해결 건을 잠가 제한된 개수만 선점한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT safetyCase
            FROM FamilySafetyCase safetyCase
            JOIN FETCH safetyCase.analysis
            JOIN FETCH safetyCase.ward
            WHERE safetyCase.status IN :statuses
              AND safetyCase.nextReminderAt IS NOT NULL
              AND safetyCase.nextReminderAt <= :now
            ORDER BY safetyCase.nextReminderAt ASC
            """)
    List<FamilySafetyCase> findDueCases(
            @Param("statuses") List<FamilySafetyStatus> statuses,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );
}
