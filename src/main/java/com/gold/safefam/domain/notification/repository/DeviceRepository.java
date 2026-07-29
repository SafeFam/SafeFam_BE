package com.gold.safefam.domain.notification.repository;

import com.gold.safefam.domain.notification.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByUserId(Long userId);

    List<Device> findByUserIdIn(Collection<Long> userIds);

    Optional<Device> findByFcmToken(String fcmToken);
}
