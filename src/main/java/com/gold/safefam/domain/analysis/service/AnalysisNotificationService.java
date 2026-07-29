package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.event.AnalysisResultCommittedEvent;
import com.gold.safefam.domain.family.service.FamilyNotificationService;
import com.gold.safefam.domain.notification.service.FcmService;
import com.gold.safefam.domain.notification.service.NotificationRecipientReader;
import com.gold.safefam.domain.notification.service.NotificationRecipientReader.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AnalysisNotificationService.class
            );

    private final NotificationRecipientReader recipientReader;
    private final FcmService fcmService;
    private final FamilyNotificationService familyNotificationService;

    public AnalysisNotificationService(
            NotificationRecipientReader recipientReader,
            FcmService fcmService,
            FamilyNotificationService familyNotificationService
    ) {
        this.recipientReader = recipientReader;
        this.fcmService = fcmService;
        this.familyNotificationService =
                familyNotificationService;
    }

    /* 분석 결과 기반 종합 알림 전송 */
    public void notifyResult(
            AnalysisResultCommittedEvent event
    ) {

        // 분석을 요청한 사용자 본인 기기들에게 FCM 알림 전송
        notifyOwner(event);

        // 고위험(HIGH) 피싱 위험 탐지 시 보호자에게 알림 전송
        if (event.isHighRisk()) {
            notifyGuardians(event);
        }
    }

    /* 분석 요청 사용자 본인 FCM 알림 발송 */
    private void notifyOwner(
            AnalysisResultCommittedEvent event
    ) {
        List<Recipient> recipients =
                recipientReader.findOwnerDevices(event.userId());

        if (recipients.isEmpty()) {
            log.debug(
                    "No registered device for analysis notification. "
                            + "userId={}, analysisId={}",
                    event.userId(),
                    event.analysisId()
            );
            return;
        }

        NotificationMessage message =
                createMessage(event);

        // 등록된 개별 기기별로 FCM 발송 시도
        for (Recipient recipient : recipients) {
            try {
                fcmService.sendNotification(
                        recipient.fcmToken(),
                        message.title(),
                        message.body(),
                        event.analysisId()
                );
            } catch (RuntimeException exception) {

                // 예외 격리: 특정 기기 하나로의 발송 실패가 다른 기기 대상 푸시 발송을 중단 X
                log.warn(
                        "Failed to send analysis notification. "
                                + "userId={}, analysisId={}, deviceId={}",
                        event.userId(),
                        event.analysisId(),
                        recipient.deviceId(),
                        exception
                );
            }
        }
    }

    /* 연결된 보호자 대상 고위험 미러링 알림 발송 */
    private void notifyGuardians(
            AnalysisResultCommittedEvent event
    ) {
        try {
            familyNotificationService
                    .mirrorHighRiskToGuardians(
                            event.userId(),
                            event.explanation(),
                            event.analysisId()
                    );
        } catch (RuntimeException exception) {

            // 예외 격리: 예외가 발생해도 본인 대상 알림 로그나 후속 작업에 영향 X
            log.warn(
                    "Failed to notify guardians. "
                            + "userId={}, analysisId={}",
                    event.userId(),
                    event.analysisId(),
                    exception
            );
        }
    }

    /* 이벤트 상태별 푸시 알림 타이틀 & 본문 생성 */
    private NotificationMessage createMessage(
            AnalysisResultCommittedEvent event
    ) {
        // 분석 전체 실패
        if (event.status() == AnalysisStatus.FAILED) {
            return new NotificationMessage(
                    "문자 분석에 실패했어요",
                    "분석을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요."
            );
        }

        // 분석 부분 성공
        if (event.status()
                == AnalysisStatus.PARTIAL_SUCCESS) {
            return createPartialMessage(event.riskLevel());
        }

        // 분석 정상 완료
        return createCompletedMessage(event.riskLevel());
    }

    /* 정상 완료 상태의 위험 등급별 안내 문구 생성 */
    private NotificationMessage createCompletedMessage(
            RiskLevel riskLevel
    ) {
        if (riskLevel == null) {
            return new NotificationMessage(
                    "문자 분석이 완료됐어요",
                    "분석 결과를 확인해 주세요."
            );
        }

        return switch (riskLevel) {
            case HIGH -> new NotificationMessage(
                    "위험 문자가 탐지됐어요",
                    "피싱 위험이 높은 문자입니다. 즉시 확인해 주세요."
            );

            case MEDIUM -> new NotificationMessage(
                    "주의가 필요한 문자예요",
                    "의심스러운 요소가 탐지됐습니다. 결과를 확인해 주세요."
            );

            case LOW -> new NotificationMessage(
                    "문자 분석이 완료됐어요",
                    "현재 분석 결과는 위험도가 낮습니다."
            );
        };
    }

    /* 부분 성공 상태의 위험 등급별 안내 문구 생성 */
    private NotificationMessage createPartialMessage(
            RiskLevel riskLevel
    ) {
        if (riskLevel == RiskLevel.HIGH) {
            return new NotificationMessage(
                    "위험 문자가 탐지됐어요",
                    "일부 분석은 완료되지 않았지만 높은 위험이 탐지됐습니다."
            );
        }

        return new NotificationMessage(
                "문자 분석이 일부 완료됐어요",
                "일부 분석을 완료하지 못했습니다. 상세 결과를 확인해 주세요."
        );
    }

    private record NotificationMessage(
            String title,
            String body
    ) {
    }
}
