package com.gold.safefam.domain.notification.service;

import com.gold.safefam.domain.family.repository.FamilyLinkRepository;
import com.gold.safefam.domain.notification.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationRecipientReader {

    private final FamilyLinkRepository familyLinkRepository;
    private final DeviceRepository deviceRepository;

    public NotificationRecipientReader(
            FamilyLinkRepository familyLinkRepository,
            DeviceRepository deviceRepository
    ) {
        this.familyLinkRepository = familyLinkRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional(readOnly = true)
    public List<Recipient> findOwnerDevices(Long userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(device -> new Recipient(
                        device.getUserId(),
                        device.getId(),
                        device.getFcmToken()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Recipient> findGuardianDevices(Long wardId) {
        List<Long> guardianIds =
                familyLinkRepository.findActiveByWardId(wardId)
                        .stream()
                        .map(link -> link.getProtector().getId())
                        .distinct()
                        .toList();

        if (guardianIds.isEmpty()) {
            return List.of();
        }

        return deviceRepository.findByUserIdIn(guardianIds).stream()
                .map(device -> new Recipient(
                        device.getUserId(),
                        device.getId(),
                        device.getFcmToken()
                ))
                .toList();
    }

    public record Recipient(
            Long userId,
            Long deviceId,
            String fcmToken
    ) {
    }
}
