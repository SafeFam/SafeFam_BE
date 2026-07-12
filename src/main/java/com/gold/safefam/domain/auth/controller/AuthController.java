package com.gold.safefam.domain.auth.controller;

import com.gold.safefam.domain.auth.dto.LoginRequest;
import com.gold.safefam.domain.auth.dto.LogoutRequest;
import com.gold.safefam.domain.auth.dto.ReissueRequest;
import com.gold.safefam.domain.auth.dto.SignupRequest;
import com.gold.safefam.domain.auth.dto.TokenResponse;
import com.gold.safefam.domain.auth.service.AuthService;
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

/**
 * 로그인 요청이 들어오면 AuthController에서 처리
 * AuthService에서 이메일/비밀번호 확인
 * JwtUtil에서 Access/Refresh Token 생성
 * Refresh Token 해시를 DB에 저장
 * 토큰 반환
 */

@Tag(name = "1. 인증", description = "회원가입, 로그인 및 JWT 관리")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 SafeFam 계정을 생성합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인", description = "인증 성공 시 Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", response));
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
}
