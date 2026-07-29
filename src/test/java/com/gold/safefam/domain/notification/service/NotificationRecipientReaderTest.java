package com.gold.safefam.domain.notification.service;

import com.gold.safefam.domain.family.entity.FamilyLink;
import com.gold.safefam.domain.family.repository.FamilyLinkRepository;
import com.gold.safefam.domain.notification.entity.Device;
import com.gold.safefam.domain.notification.enums.DevicePlatform;
import com.gold.safefam.domain.notification.repository.DeviceRepository;
import com.gold.safefam.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRecipientReaderTest {

    private FamilyLinkRepository familyLinkRepository;
    private DeviceRepository deviceRepository;
    private NotificationRecipientReader reader;

    @BeforeEach
    void setUp() {
        familyLinkRepository = mock(FamilyLinkRepository.class);
        deviceRepository = mock(DeviceRepository.class);
        reader = new NotificationRecipientReader(
                familyLinkRepository,
                deviceRepository
        );
    }

    @Test
    void returnsScalarOwnerDeviceData() {
        when(deviceRepository.findByUserId(10L)).thenReturn(
                List.of(new Device(
                        10L, "owner-token", DevicePlatform.ANDROID
                ))
        );

        List<NotificationRecipientReader.Recipient> recipients =
                reader.findOwnerDevices(10L);

        assertEquals(1, recipients.size());
        assertEquals(10L, recipients.get(0).userId());
        assertEquals("owner-token", recipients.get(0).fcmToken());
    }

    @Test
    void deduplicatesGuardiansBeforeLoadingDevices() {
        FamilyLink first = linkWithProtector(20L);
        FamilyLink duplicate = linkWithProtector(20L);
        FamilyLink second = linkWithProtector(30L);
        when(familyLinkRepository.findActiveByWardId(10L))
                .thenReturn(List.of(first, duplicate, second));
        when(deviceRepository.findByUserIdIn(
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(List.of(
                new Device(20L, "guardian-token", DevicePlatform.ANDROID)
        ));

        reader.findGuardianDevices(10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(deviceRepository).findByUserIdIn(captor.capture());
        assertEquals(List.of(20L, 30L), captor.getValue().stream().toList());
    }

    @Test
    void skipsDeviceQueryWhenNoActiveGuardianExists() {
        when(familyLinkRepository.findActiveByWardId(10L))
                .thenReturn(List.of());

        assertEquals(List.of(), reader.findGuardianDevices(10L));
        verify(deviceRepository, never()).findByUserIdIn(
                org.mockito.ArgumentMatchers.anyCollection()
        );
    }

    private FamilyLink linkWithProtector(Long protectorId) {
        FamilyLink link = mock(FamilyLink.class);
        User protector = mock(User.class);
        when(protector.getId()).thenReturn(protectorId);
        when(link.getProtector()).thenReturn(protector);
        return link;
    }
}
