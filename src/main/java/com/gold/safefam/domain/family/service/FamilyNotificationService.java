package com.gold.safefam.domain.family.service;

import com.gold.safefam.domain.family.repository.FamilyLinkRepository;
import com.gold.safefam.domain.family.safety.dto.FamilySafetyCaseCreationResult;
import com.gold.safefam.domain.family.safety.dto.FamilySafetyNotificationTarget;
import com.gold.safefam.domain.family.safety.service.FamilySafetyCaseService;
import com.gold.safefam.domain.family.safety.service.FamilySafetyNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 분석 도메인의 HIGH 결과와 가족 안전 대응 도메인을 연결하는 진입점이다.
 * 활성 가족 관계가 있을 때만 공동 대응 건을 생성하고 최초 보호자 FCM을 발송한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyNotificationService {

    private final FamilySafetyCaseService safetyCaseService;
    private final FamilySafetyNotificationService notificationService;
    private final FamilyLinkRepository familyLinkRepository;

    /** 동일 HIGH 이벤트에서는 대응 건과 최초 FCM이 각각 한 번만 처리되도록 조율한다. */
    public void mirrorHighRiskToGuardians(Long wardId, String explanation, Long analysisId) {
        if (familyLinkRepository.findActiveByWardId(wardId).isEmpty()) {
            return;
        }

        FamilySafetyCaseCreationResult creation = safetyCaseService.createForHighRisk(wardId, analysisId);
        if (!creation.created()) {
            return;
        }

        FamilySafetyNotificationTarget target = creation.target();
        if (notificationService.send(target, false)) {
            safetyCaseService.recordNotificationDelivered(target.caseId());
        }
    }
}
