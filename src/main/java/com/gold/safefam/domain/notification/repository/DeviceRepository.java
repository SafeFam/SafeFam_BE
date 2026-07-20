package com.gold.safefam.domain.notification.repository;

import com.gold.safefam.domain.notification.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByUserId(Long userId);

    boolean existsByUserIdAndFcmToken(Long userId, String fcmToken);
}