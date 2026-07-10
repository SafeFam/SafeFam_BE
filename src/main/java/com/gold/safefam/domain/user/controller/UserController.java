package com.gold.safefam.domain.user.controller;

import com.gold.safefam.domain.user.dto.UpdateUserRequest;
import com.gold.safefam.domain.user.dto.UpdateUserSettingsRequest;
import com.gold.safefam.domain.user.dto.UserResponse;
import com.gold.safefam.domain.user.dto.UserSettingsResponse;
import com.gold.safefam.domain.user.dto.WithdrawalRequest;
import com.gold.safefam.global.config.SwaggerConfig;
import com.gold.safefam.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. 사용자", description = "내 정보 및 탐지·알림 설정 관리")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    @Operation(summary = "내 정보 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(summary = "내 정보 수정", description = "현재는 사용자 이름을 수정합니다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(summary = "탐지·알림 설정 조회")
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getSettings() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(
            summary = "탐지·알림 설정 변경",
            description = "전달한 필드만 변경합니다. 문자 원문 저장은 기본적으로 비활성화합니다."
    )
    @PatchMapping("/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(summary = "회원 탈퇴", description = "사용자와 탐지 이력을 개인정보 처리 정책에 따라 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @Valid @RequestBody WithdrawalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
