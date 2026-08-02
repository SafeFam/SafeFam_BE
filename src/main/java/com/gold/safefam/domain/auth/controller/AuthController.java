package com.gold.safefam.domain.auth.controller;

import com.gold.safefam.domain.auth.dto.*;
import com.gold.safefam.domain.auth.service.AuthService;
import com.gold.safefam.domain.auth.service.PhoneVerificationService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 로그인 요청이 들어오면 AuthController에서 처리
 * AuthService에서 전화번호/비밀번호 확인
 * JwtUtil에서 Access/Refresh Token 생성
 * Refresh Token 해시를 DB에 저장
 * 토큰 반환
 */

@Tag(name = "01. 인증", description = "회원가입, 로그인 및 JWT 관리")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;

    // 회원가입 전에 휴대폰 소유 여부를 확인하기 위한 인증번호 발송
    @Operation(summary = "휴대폰 인증번호 발송", description = "회원가입에 사용할 휴대폰 번호로 6자리 인증번호를 발송합니다.")
    @PostMapping("/phone-verifications/send")
    public ResponseEntity<ApiResponse<Void>> sendPhoneVerification(
            @Valid @RequestBody SendPhoneVerificationRequest request
    ) {
        phoneVerificationService.sendCode(request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.success("인증번호를 발송했습니다."));
    }

    // 발송된 인증번호를 검증하고 해당 휴대폰 번호를 회원가입 가능한 상태로 변경함
    @Operation(summary = "휴대폰 인증번호 검증", description = "SMS로 받은 6자리 인증번호를 검증합니다.")
    @PostMapping("/phone-verifications/verify")
    public ResponseEntity<ApiResponse<Void>> verifyPhone(
            @Valid @RequestBody VerifyPhoneRequest request
    ) {
        phoneVerificationService.verifyCode(request.phoneNumber(), request.code());
        return ResponseEntity.ok(ApiResponse.success("휴대폰 인증이 완료되었습니다."));
    }

    @Operation(summary = "회원가입", description = "인증된 전화번호와 비밀번호, 이름으로 SafeFam 계정을 생성합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인", description = "전화번호와 비밀번호 인증 성공 시 Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", response));
    }

    @Operation(summary = "비밀번호 재설정", description = "휴대폰 인증 완료 후 새 비밀번호로 변경합니다.")
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다."));
    }

    @Operation(summary = "계정 잠금 해제", description = "휴대폰 인증으로 잠긴 계정을 해제합니다.")
    @PostMapping("/unlock")
    public ResponseEntity<ApiResponse<Void>> unlock(
            @Valid @RequestBody UnlockRequest request
    ) {
        authService.unlock(request);
        return ResponseEntity.ok(ApiResponse.success("계정 잠금이 해제되었습니다."));
    }

    @Operation(summary = "Access Token 재발급", description = "유효한 Refresh Token으로 토큰을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
        TokenResponse response = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
    }

    @Operation(
            summary = "로그아웃",
            description = "저장된 Refresh Token과 기기 세션을 무효화합니다.",
            security = @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(userId, request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다."));
    }

    @Operation(summary = "카카오 소셜 로그인", description = "카카오 액세스 토큰으로 로그인하거나 신규 사용자 여부를 반환합니다.")
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<Map<String, Object>>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request
    ) {
        Map<String, Object> response = authService.kakaoLogin(request.kakaoAccessToken());
        return ResponseEntity.ok(ApiResponse.success("카카오 로그인 처리가 완료되었습니다.", response));
    }

    @Operation(summary = "카카오 회원가입", description = "카카오 ID와 전화번호 인증으로 계정을 생성하거나 기존 계정에 연동합니다.")
    @PostMapping("/kakao/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoSignup(
            @Valid @RequestBody KakaoSignupRequest request
    ) {
        TokenResponse response = authService.kakaoSignup(request);
        return ResponseEntity.ok(ApiResponse.success("카카오 회원가입이 완료되었습니다.", response));
    }
}
