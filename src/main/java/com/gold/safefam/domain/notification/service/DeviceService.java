package com.gold.safefam.domain.notification.service;

import com.gold.safefam.domain.notification.dto.DeviceResponse;
import com.gold.safefam.domain.notification.dto.RegisterDeviceRequest;
import com.gold.safefam.domain.notification.entity.Device;
import com.gold.safefam.domain.notification.repository.DeviceRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional
    public DeviceResponse register(Long userId, RegisterDeviceRequest request) {
        if (deviceRepository.existsByUserIdAndFcmToken(userId, request.deviceToken())) {
            Device existing = deviceRepository.findByUserId(userId).stream()
                    .filter(d -> d.getFcmToken().equals(request.deviceToken()))
                    .findFirst()
                    .orElseThrow();
            return new DeviceResponse(existing.getId(), existing.getPlatform());
        }

        Device device = new Device(userId, request.deviceToken(), request.platform());
        deviceRepository.save(device);
        return new DeviceResponse(device.getId(), device.getPlatform());
    }

    @Transactional
    public void unregister(Long userId, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (!device.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        deviceRepository.delete(device);
    }
}