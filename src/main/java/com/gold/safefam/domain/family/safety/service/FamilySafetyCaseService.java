package com.gold.safefam.domain.family.safety.service;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.privacy.PiiMaskingService;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.family.enums.FamilyLinkStatus;
import com.gold.safefam.domain.family.repository.FamilyLinkRepository;
import com.gold.safefam.domain.family.safety.dto.FamilyCallResponse;
import com.gold.safefam.domain.family.safety.dto.FamilySafetyCaseCreationResult;
import com.gold.safefam.domain.family.safety.dto.FamilySafetyCaseResponse;
import com.gold.safefam.domain.family.safety.dto.FamilySafetyNotificationTarget;
import com.gold.safefam.domain.family.safety.entity.FamilySafetyCase;
import com.gold.safefam.domain.family.safety.enums.FamilySafetyStatus;
import com.gold.safefam.domain.family.safety.repository.FamilySafetyCaseRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import com.gold.safefam.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 가족 공동 안전 대응 건의 생성, 조회, 상태 전이와 재알림 선점을 담당한다.
 * 보호자 API의 모든 접근에서 ACTIVE 가족 관계를 검증하고,
 * 외부에 제공할 문자 정보는 PII 마스킹 후 대응 건에 스냅샷으로 저장한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilySafetyCaseService {

    private static final List<FamilySafetyStatus> UNRESOLVED_STATUSES = List.of(
            FamilySafetyStatus.PENDING,
            FamilySafetyStatus.CONTACTING
    );

    private final FamilySafetyCaseRepository safetyCaseRepository;
    private final FamilyLinkRepository familyLinkRepository;
    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final PiiMaskingService piiMaskingService;

    @Value("${safefam.family-safety.reminder-delay-minutes:10}")
    private long reminderDelayMinutes;

    /**
     * HIGH 분석 행을 잠근 뒤 공동 대응 건을 한 번만 생성한다.
     * 생성 여부를 함께 반환해 중복 이벤트의 최초 FCM 재발송을 방지한다.
     */
    @Transactional
    public FamilySafetyCaseCreationResult createForHighRisk(Long wardId, Long analysisId) {
        Analysis analysis = analysisRepository.findByIdForUpdate(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
        return safetyCaseRepository.findByAnalysisId(analysisId)
                .map(safetyCase -> new FamilySafetyCaseCreationResult(
                        toNotificationTarget(safetyCase),
                        false
                ))
                .orElseGet(() -> new FamilySafetyCaseCreationResult(
                        createNewCase(wardId, analysis),
                        true
                ));
    }

    /** 한 대 이상의 보호자 기기에 FCM 전달이 성공한 시각을 저장한다. */
    @Transactional
    public void recordNotificationDelivered(Long caseId) {
        safetyCaseRepository.findByIdForUpdate(caseId)
                .ifPresent(safetyCase -> safetyCase.recordNotificationDelivered(OffsetDateTime.now()));
    }

    /** 현재 보호자가 접근할 수 있는 공동 대응 건을 상태 조건과 함께 조회한다. */
    public PageResponse<FamilySafetyCaseResponse> getCases(
            Long guardianId,
            FamilySafetyStatus status,
            int page,
            int size
    ) {
        Page<FamilySafetyCase> result = safetyCaseRepository.findAccessibleCases(
                guardianId,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return new PageResponse<>(
                result.getContent().stream().map(FamilySafetyCaseResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isLast()
        );
    }

    /** ACTIVE 가족 관계를 검증한 뒤 공동 대응 건 상세 정보를 반환한다. */
    public FamilySafetyCaseResponse getCase(Long guardianId, Long caseId) {
        FamilySafetyCase safetyCase = safetyCaseRepository.findDetailedById(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_SAFETY_CASE_NOT_FOUND));
        validateGuardianAccess(guardianId, safetyCase.getWard().getId());
        return FamilySafetyCaseResponse.from(safetyCase);
    }

    /** 보호자의 통화 시도를 기록하고 앱이 전화를 걸 수 있도록 가족 번호를 반환한다. */
    @Transactional
    public FamilyCallResponse startCall(Long guardianId, Long caseId) {
        FamilySafetyCase safetyCase = findForAction(caseId);
        validateGuardianAccess(guardianId, safetyCase.getWard().getId());
        User guardian = findUser(guardianId);

        try {
            safetyCase.startCall(guardian, OffsetDateTime.now());
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.FAMILY_SAFETY_ALREADY_RESOLVED);
        }

        return new FamilyCallResponse(
                safetyCase.getId(),
                safetyCase.getWard().getId(),
                safetyCase.getWard().getName(),
                safetyCase.getWard().getPhoneNumber()
        );
    }

    /** 보호자가 선택한 안전 확인 결과를 확정하고 최종 처리자를 기록한다. */
    @Transactional
    public FamilySafetyCaseResponse resolve(
            Long guardianId,
            Long caseId,
            FamilySafetyStatus resolution
    ) {
        if (resolution == null || !resolution.isResolved()) {
            throw new BusinessException(ErrorCode.FAMILY_SAFETY_INVALID_STATUS);
        }

        FamilySafetyCase safetyCase = findForAction(caseId);
        validateGuardianAccess(guardianId, safetyCase.getWard().getId());
        User guardian = findUser(guardianId);
        try {
            safetyCase.resolve(resolution, guardian, OffsetDateTime.now());
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.FAMILY_SAFETY_ALREADY_RESOLVED);
        }
        return FamilySafetyCaseResponse.from(safetyCase);
    }

    /**
     * 재알림 시간이 지난 미해결 건을 잠가 선점하고 다음 알림 시각을 예약한다.
     * 반환된 최소 정보는 트랜잭션 밖에서 FCM 발송에 사용된다.
     */
    @Transactional
    public List<FamilySafetyNotificationTarget> claimDueReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        List<FamilySafetyCase> dueCases = safetyCaseRepository.findDueCases(
                UNRESOLVED_STATUSES, now, PageRequest.of(0, 100)
        );
        OffsetDateTime nextReminderAt = now.plusMinutes(reminderDelayMinutes);
        return dueCases.stream()
                .peek(safetyCase -> safetyCase.claimReminder(now, nextReminderAt))
                .map(this::toNotificationTarget)
                .toList();
    }

    private FamilySafetyNotificationTarget createNewCase(Long wardId, Analysis analysis) {
        if (!analysis.getUserId().equals(wardId) || analysis.getRiskLevel() != RiskLevel.HIGH) {
            throw new IllegalArgumentException("Only the ward's HIGH analysis can create a safety case");
        }

        User ward = findUser(wardId);
        OffsetDateTime now = OffsetDateTime.now();
        FamilySafetyCase safetyCase = FamilySafetyCase.create(
                analysis,
                ward,
                suspectedInstitution(analysis),
                riskyAction(analysis),
                safeText(analysis.getContentPreview(), 200),
                safeText(analysis.getExplanation(), 1000),
                now,
                now.plusMinutes(reminderDelayMinutes)
        );
        return toNotificationTarget(safetyCaseRepository.saveAndFlush(safetyCase));
    }

    private FamilySafetyCase findForAction(Long caseId) {
        return safetyCaseRepository.findByIdForUpdate(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_SAFETY_CASE_NOT_FOUND));
    }

    private void validateGuardianAccess(Long guardianId, Long wardId) {
        boolean linked = familyLinkRepository.existsByProtectorIdAndWardIdAndStatus(
                guardianId, wardId, FamilyLinkStatus.ACTIVE
        );
        if (!linked) {
            throw new BusinessException(ErrorCode.FAMILY_LINK_FORBIDDEN);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private FamilySafetyNotificationTarget toNotificationTarget(FamilySafetyCase safetyCase) {
        return new FamilySafetyNotificationTarget(
                safetyCase.getId(),
                safetyCase.getAnalysis().getId(),
                safetyCase.getWard().getId(),
                safetyCase.getWard().getName(),
                safetyCase.getRiskScore(),
                safetyCase.getRiskyAction()
        );
    }

    private String suspectedInstitution(Analysis analysis) {
        if (analysis.getSender() != null && !analysis.getSender().isBlank()) {
            return safeText(analysis.getSender(), 100);
        }
        PhishingCategory category = analysis.getCategory();
        if (category == null) {
            return "기관 불명";
        }
        return switch (category) {
            case FINANCIAL_INSTITUTION -> "금융기관 사칭 의심";
            case GOVERNMENT_AGENCY -> "공공기관 사칭 의심";
            case LOAN -> "대출기관 사칭 의심";
            case JOB -> "채용기관 사칭 의심";
            case DELIVERY -> "배송업체 사칭 의심";
            case MESSENGER -> "메신저 지인 사칭 의심";
            case OTHER -> "기관 불명";
        };
    }

    private String riskyAction(Analysis analysis) {
        String actions = analysis.getIndicators().stream()
                .map(indicator -> safeText(indicator.getDescription(), 160))
                .distinct()
                .limit(3)
                .reduce((left, right) -> left + " · " + right)
                .orElse(null);
        return actions != null && !actions.isBlank()
                ? abbreviate(actions, 500)
                : safeText(analysis.getExplanation(), 500);
    }

    private String safeText(String value, int maxLength) {
        String masked = piiMaskingService.mask(value);
        if (masked == null || masked.isBlank()) {
            return "탐지 근거를 확인해 주세요.";
        }
        return abbreviate(masked, maxLength);
    }

    private String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength - 3) + "...";
    }
}
