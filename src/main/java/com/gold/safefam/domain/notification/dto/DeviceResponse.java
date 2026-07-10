package com.gold.safefam.domain.notification.dto;

import com.gold.safefam.domain.notification.enums.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "등록된 푸시 알림 기기")
public record DeviceResponse(
        @Schema(example = "10")
        Long deviceId,

        DevicePlatform platform
) {
}
