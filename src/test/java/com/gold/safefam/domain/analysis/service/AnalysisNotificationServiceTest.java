package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.event.AnalysisResultCommittedEvent;
import com.gold.safefam.domain.family.service.FamilyNotificationService;
import com.gold.safefam.domain.notification.service.FcmService;
import com.gold.safefam.domain.notification.service.NotificationRecipientReader;
import com.gold.safefam.domain.notification.service.NotificationRecipientReader.Recipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisNotificationServiceTest {

    private NotificationRecipientReader recipientReader;
    private FcmService fcmService;
    private FamilyNotificationService familyNotificationService;
    private AnalysisNotificationService service;

    @BeforeEach
    void setUp() {
        recipientReader = mock(NotificationRecipientReader.class);
        fcmService = mock(FcmService.class);
        familyNotificationService =
                mock(FamilyNotificationService.class);
        service = new AnalysisNotificationService(
                recipientReader,
                fcmService,
                familyNotificationService
        );
    }

    @Test
    void notifiesOwnerAndGuardiansForHighRiskResult() {
        when(recipientReader.findOwnerDevices(10L)).thenReturn(
                List.of(new Recipient(10L, 1L, "owner-token"))
        );
        AnalysisResultCommittedEvent event = event(
                AnalysisStatus.COMPLETED,
                RiskLevel.HIGH
        );

        service.notifyResult(event);

        verify(fcmService).sendNotification(
                eq("owner-token"), anyString(), anyString(), eq(1L)
        );
        verify(familyNotificationService)
                .mirrorHighRiskToGuardians(10L, "reason", 1L);
    }

    @Test
    void doesNotNotifyGuardiansForLowRiskResult() {
        when(recipientReader.findOwnerDevices(10L))
                .thenReturn(List.of());

        service.notifyResult(event(
                AnalysisStatus.COMPLETED,
                RiskLevel.LOW
        ));

        verify(familyNotificationService, never())
                .mirrorHighRiskToGuardians(
                        eq(10L), anyString(), eq(1L)
                );
    }

    @Test
    void continuesWithOtherOwnerDevicesAfterOneFailure() {
        when(recipientReader.findOwnerDevices(10L)).thenReturn(
                List.of(
                        new Recipient(10L, 1L, "bad-token"),
                        new Recipient(10L, 2L, "good-token")
                )
        );
        doThrow(new IllegalStateException("FCM unavailable"))
                .when(fcmService)
                .sendNotification(
                        eq("bad-token"),
                        anyString(),
                        anyString(),
                        eq(1L)
                );

        service.notifyResult(event(
                AnalysisStatus.COMPLETED,
                RiskLevel.LOW
        ));

        verify(fcmService).sendNotification(
                eq("good-token"), anyString(), anyString(), eq(1L)
        );
    }

    private AnalysisResultCommittedEvent event(
            AnalysisStatus status,
            RiskLevel riskLevel
    ) {
        return new AnalysisResultCommittedEvent(
                1L, 10L, status, riskLevel, "reason"
        );
    }
}
