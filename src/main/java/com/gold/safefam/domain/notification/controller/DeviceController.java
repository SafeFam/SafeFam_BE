package com.gold.safefam.domain.notification.controller;

import com.gold.safefam.domain.notification.dto.DeviceResponse;
import com.gold.safefam.domain.notification.dto.RegisterDeviceRequest;
import com.gold.safefam.domain.notification.service.DeviceService;
import com.gold.safefam.global.config.SwaggerConfig;
import com.gold.safefam.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "5. 푸시 알림 기기", description = "분석 완료 알림을 받을 모바일 기기 관리")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "푸시 알림 기기 등록", description = "동일 토큰은 중복 생성하지 않고 갱신합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceResponse>> register(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegisterDeviceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("기기가 등록되었습니다.", deviceService.register(userId, request)));
    }

    @Operation(summary = "푸시 알림 기기 해제")
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long deviceId
    ) {
        deviceService.unregister(userId, deviceId);
        return ResponseEntity.noContent().build();
    }
}
