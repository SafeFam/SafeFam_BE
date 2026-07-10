package com.gold.safefam.domain.notification.dto;

import com.gold.safefam.domain.notification.enums.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "푸시 알림 기기 등록 요청")
public record RegisterDeviceRequest(
        @NotBlank
        @Size(max = 512)
        @Schema(description = "FCM/APNs 기기 토큰", example = "fcm-device-token")
        String deviceToken,

        @NotNull
        @Schema(example = "ANDROID")
        DevicePlatform platform
) {
}
