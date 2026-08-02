package com.gold.safefam.domain.family.safety.scheduler;

import com.gold.safefam.domain.family.safety.dto.FamilySafetyNotificationTarget;
import com.gold.safefam.domain.family.safety.service.FamilySafetyCaseService;
import com.gold.safefam.domain.family.safety.service.FamilySafetyNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 설정된 주기마다 처리되지 않은 공동 대응 건을 선점해 보호자에게 재알림한다.
 * 종료 상태는 조회 대상에서 제외되므로 안전 확인 이후에는 알림이 자동으로 중단된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FamilySafetyReminderScheduler {

    private final FamilySafetyCaseService safetyCaseService;
    private final FamilySafetyNotificationService notificationService;

    /** 미해결 건을 최대 100개씩 처리하고 실제 FCM 성공 시각을 기록한다. */
    @Scheduled(fixedDelayString = "${safefam.family-safety.scheduler-delay-ms:60000}")
    public void remindUnresolvedCases() {
        List<FamilySafetyNotificationTarget> targets = safetyCaseService.claimDueReminders();
        for (FamilySafetyNotificationTarget target : targets) {
            if (notificationService.send(target, true)) {
                safetyCaseService.recordReminderDelivered(target.caseId());
            }
        }
        if (!targets.isEmpty()) {
            log.info("가족 안전 재알림 처리 완료 — count={}", targets.size());
        }
    }
}
