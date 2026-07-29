package com.gold.safefam.domain.analysis.event;

import com.gold.safefam.domain.analysis.service.AnalysisNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AnalysisResultNotificationListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AnalysisResultNotificationListener.class
            );

    private final AnalysisNotificationService notificationService;

    public AnalysisResultNotificationListener(
            AnalysisNotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    /* 분석 결과 알림 이벤트 처리 */
    @Async("analysisNotificationExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            AnalysisResultCommittedEvent event
    ) {
        try {
            // 알림 발송 서비스 호출
            notificationService.notifyResult(event);

            log.info(
                    "Analysis notification processed. "
                            + "analysisId={}, userId={}, "
                            + "status={}, riskLevel={}",
                    event.analysisId(),
                    event.userId(),
                    event.status(),
                    event.riskLevel()
            );
        } catch (RuntimeException exception) {

            log.error(
                    "Analysis notification failed after commit. "
                            + "analysisId={}, userId={}",
                    event.analysisId(),
                    event.userId(),
                    exception
            );
        }
    }
}