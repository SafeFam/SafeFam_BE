package com.gold.safefam.domain.family.service;

import com.gold.safefam.domain.family.entity.FamilyLink;
import com.gold.safefam.domain.family.repository.FamilyLinkRepository;
import com.gold.safefam.domain.notification.entity.Device;
import com.gold.safefam.domain.notification.repository.DeviceRepository;
import com.gold.safefam.domain.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyNotificationService {

    private final FamilyLinkRepository familyLinkRepository;
    private final DeviceRepository deviceRepository;
    private final FcmService fcmService;

    @Async
    @Transactional(readOnly = true)
    public void mirrorHighRiskToGuardians(Long wardId, String explanation, Long analysisId) {
        List<FamilyLink> links = familyLinkRepository.findActiveByWardId(wardId);

        if (links.isEmpty()) {
            return;
        }

        for (FamilyLink link : links) {
            Long guardianId = link.getProtector().getId();
            List<Device> devices = deviceRepository.findByUserId(guardianId);

            if (devices.isEmpty()) {
                continue;
            }

            devices.forEach(device -> {
                try {
                    fcmService.sendNotification(
                            device.getFcmToken(),
                            "⚠️ 가족 위험 문자 탐지",
                            "보호 중인 가족에게 피싱 위험 문자가 탐지되었습니다.",
                            analysisId
                    );
                } catch (Exception e) {
                    log.warn("보호자 FCM 미러링 실패 — guardianId={}, wardId={}", guardianId, wardId, e);
                }
            });
        }
    }
}