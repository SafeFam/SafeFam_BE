package com.gold.safefam.domain.family.service;

import com.gold.safefam.domain.notification.service.FcmService;
import com.gold.safefam.domain.notification.service.NotificationRecipientReader;
import com.gold.safefam.domain.notification.service.NotificationRecipientReader.Recipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyNotificationService {

    private final NotificationRecipientReader recipientReader;
    private final FcmService fcmService;

    public void mirrorHighRiskToGuardians(Long wardId, String explanation, Long analysisId) {
        List<Recipient> recipients =
                recipientReader.findGuardianDevices(wardId);

        if (recipients.isEmpty()) {
            return;
        }

        for (Recipient recipient : recipients) {
            try {
                fcmService.sendNotification(
                        recipient.fcmToken(),
                        "⚠️ 가족 위험 문자 탐지",
                        "보호 중인 가족에게 피싱 위험 문자가 탐지되었습니다.",
                        analysisId
                );
            } catch (Exception e) {
                log.warn(
                        "보호자 FCM 미러링 실패 — guardianId={}, wardId={}",
                        recipient.userId(),
                        wardId,
                        e
                );
            }
        }
    }
}
