package com.gold.safefam.domain.notification.repository;

import com.gold.safefam.domain.notification.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByUserId(Long userId);

    Optional<Device> findByFcmToken(String fcmToken);
}