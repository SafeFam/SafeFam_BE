package com.gold.safefam.domain.family.safety.service;

import com.gold.safefam.domain.family.safety.dto.FamilySafetyNotificationTarget;
import com.gold.safefam.domain.notification.service.FcmService;
import com.gold.safefam.domain.notification.service.NotificationRecipientReader;
import com.gold.safefam.domain.notification.service.NotificationRecipientReader.Recipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 공동 대응 건의 최초 알림과 재알림을 연결된 모든 보호자 기기로 발송한다.
 * 잠금 화면에는 문자 내용이나 가족 이름을 노출하지 않고 위험도와 앱 확인 안내만 제공한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilySafetyNotificationService {

    private final NotificationRecipientReader recipientReader;
    private final FcmService fcmService;

    /** 보호자 기기별 발송 실패를 격리하며, 한 건 이상 성공했는지를 반환한다. */
    public boolean send(FamilySafetyNotificationTarget target, boolean reminder) {
        List<Recipient> recipients = recipientReader.findGuardianDevices(target.wardId());
        boolean delivered = false;

        for (Recipient recipient : recipients) {
            try {
                delivered |= fcmService.sendFamilySafetyNotification(
                        recipient.fcmToken(),
                        reminder ? "SafeFam 안전 확인 재알림" : "가족 위험 문자 감지",
                        buildBody(target, reminder),
                        target.analysisId(),
                        target.caseId()
                );
            } catch (RuntimeException exception) {
                log.warn(
                        "가족 안전 FCM 발송 실패 — caseId={}, guardianId={}, wardId={}",
                        target.caseId(),
                        recipient.userId(),
                        target.wardId(),
                        exception
                );
            }
        }
        return delivered;
    }

    private String buildBody(FamilySafetyNotificationTarget target, boolean reminder) {
        return reminder
                ? "아직 가족의 안전 확인이 완료되지 않았습니다. SafeFam 앱에서 확인해 주세요."
                : "보호 중인 가족에게 위험도 " + target.riskScore()
                + "점의 문자가 감지되었습니다. 앱에서 확인해 주세요.";
    }
}
