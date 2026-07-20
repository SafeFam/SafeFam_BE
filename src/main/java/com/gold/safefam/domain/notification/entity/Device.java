package com.gold.safefam.domain.notification.entity;

import com.gold.safefam.domain.notification.enums.DevicePlatform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "fcm_token", nullable = false, length = 255)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Device(Long userId, String fcmToken, DevicePlatform platform) {
        this.userId = userId;
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.createdAt = LocalDateTime.now();
    }
}