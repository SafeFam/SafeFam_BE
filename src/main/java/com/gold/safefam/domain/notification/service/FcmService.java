package com.gold.safefam.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}