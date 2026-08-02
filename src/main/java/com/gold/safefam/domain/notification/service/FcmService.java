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

    /* 단일 기기 대상 FCM 푸시 알림 전송 */
    public boolean sendNotification(
            String fcmToken,
            String title,
            String body,
            Long analysisId
    ) {
        // FCM 메시지 빌드 (시각적 알림 헤더 + 백그라운드/딥링크용 데이터 페이로드)
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build()
                )
                .putData(
                        "analysisId",
                        String.valueOf(analysisId)
                )
                .build();

        // Firebase 서버로 메시지 전송 및 예외 처리
        try {
            String response =
                    FirebaseMessaging.getInstance().send(message);

            log.info(
                    "FCM notification sent: {}",
                    response
            );

            return true;
        } catch (FirebaseMessagingException exception) {
            // FCM 발송 실패
            log.warn(
                    "FCM notification failed: {}",
                    exception.getMessage()
            );

            return false;
        }
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

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Family safety FCM notification sent: {}", response);
            return true;
        } catch (FirebaseMessagingException exception) {
            log.warn("Family safety FCM notification failed: {}", exception.getMessage());
            return false;
        }
    }
}
