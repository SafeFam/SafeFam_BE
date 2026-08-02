package com.gold.safefam.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Firebase Admin SDK를 이용해 사용자 및 보호자 기기에 FCM 메시지를 전송한다.
 * Firebase 오류는 false로 변환해 알림 실패가 상위 비즈니스 트랜잭션을 롤백하지 않게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    /** 단일 기기 대상 FCM 푸시 알림 전송 */
    public boolean sendNotification(
            String fcmToken,
            String title,
            String body,
            Long analysisId
    ) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build()
                )
                .putData("analysisId", String.valueOf(analysisId))
                .build();

        return send(message, "analysis", analysisId);
    }

    /** 보호자 공동 대응 화면으로 연결되는 caseId를 데이터 페이로드에 포함해 전송한다. */
    public boolean sendFamilySafetyNotification(
            String fcmToken,
            String title,
            String body,
            Long analysisId,
            Long safetyCaseId
    ) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build()
                )
                .putData("type", "FAMILY_HIGH_RISK_ALERT")
                .putData("analysisId", String.valueOf(analysisId))
                .putData("familySafetyCaseId", String.valueOf(safetyCaseId))
                .build();

        return send(message, "family-safety", analysisId);
    }

    /** Firebase 전송과 예외 변환을 한곳에서 처리한다. */
    private boolean send(
            Message message,
            String notificationType,
            Long analysisId
    ) {
        try {
            FirebaseMessaging.getInstance().send(message);
            log.info(
                    "FCM notification sent. type={}, analysisId={}",
                    notificationType,
                    analysisId
            );
            return true;
        } catch (FirebaseMessagingException exception) {
            // 예외 원문에는 등록 토큰 등 민감 정보가 포함될 수 있어 기록하지 않는다.
            log.warn(
                    "FCM notification failed. type={}, analysisId={}, errorCode={}",
                    notificationType,
                    analysisId,
                    exception.getMessagingErrorCode()
            );
            return false;
        }
    }
}
