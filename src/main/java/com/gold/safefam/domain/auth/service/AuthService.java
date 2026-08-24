package com.gold.safefam.domain.auth.service;

import com.gold.safefam.domain.user.service.UserService;
import com.gold.safefam.global.kakao.KakaoClient;
import com.gold.safefam.domain.auth.dto.*;
import com.gold.safefam.domain.auth.entity.RefreshToken;
import com.gold.safefam.domain.auth.repository.RefreshTokenRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import com.gold.safefam.global.security.JwtUtil;
import com.gold.safefam.global.security.TokenType;
import jakarta.servlet.http.HttpServletRequest;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KakaoClient kakaoClient;
    private final PhoneVerificationService phoneVerificationService;
    private final UserService userService;

    // 회원가입
    @Transactional
    public void signup(SignupRequest request) {
        // 인증 완료 기록을 소비해 동일한 인증 결과가 여러 계정에 재사용되지 않게 함
        String phoneNumber = phoneVerificationService.consumeVerified(request.phoneNumber());
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        // 비밀번호 BCrypt 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(phoneNumber, encodedPassword, request.name());
        userRepository.save(user);
    }

    /*
    전화번호를 숫자 전용 형식으로 정규화
    전화번호로 사용자 조회
    passwordEncoder().matches()로 BCrypt 비밀번호 검증
    Access Token과 Refresh Token 발급
    Refresh Token 해시를 DB에 저장
    TokenResponse 반환

    만약 이메일이 없거나 비밀번호 틀리면 전부 INVALID_CREDENTIALS로 처리
     */

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(normalizePhoneNumber(request.phoneNumber()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }

        if (user.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            userService.incrementLoginFailCount(user.getId());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.resetLoginFail();
        return issueAndStoreTokens(user);
    }

    /*
    검증하는 역할 수행 - reissue
    JWT 서명이 정상인지
    토큰이 만료되진 않았는지
    tokenType이 REFRESH인지
    사용자 ID에 해당하는 DB 토큰이 있는지
    전달받은 토큰의 해시와 DB 해시가 같은지
    DB에 기록된 만료 시각이 지나지 않았는지

    검증 성공 시, Access/Refresh Token을 모두 새로 발급
    DB 값도 새 Refresh Token 해시로 변경되기 때문에
    이전 Refresh Token은 다시 사용할 수 없음
     */

    @Transactional
    public TokenResponse reissue(String refreshTokenValue) {
        try {
            jwtUtil.validateTokenType(refreshTokenValue, TokenType.REFRESH);
            Long userId = jwtUtil.getUserId(refreshTokenValue);
            RefreshToken storedToken = refreshTokenRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

            if (!storedToken.getTokenHash().equals(hashToken(refreshTokenValue))
                    || storedToken.getExpiresAt().isBefore(Instant.now())) {
                refreshTokenRepository.delete(storedToken);
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
            if (user.isDeleted()) {
                refreshTokenRepository.delete(storedToken);
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }
            return issueAndStoreTokens(user);
        } catch (BusinessException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Transactional
    public String logout(Long authenticatedUserId, String refreshTokenValue, HttpServletRequest httpRequest) {
        try {
            jwtUtil.validateTokenType(refreshTokenValue, TokenType.REFRESH);
            Long tokenUserId = jwtUtil.getUserId(refreshTokenValue);
            if (!authenticatedUserId.equals(tokenUserId)) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            RefreshToken storedToken = refreshTokenRepository.findByUserId(tokenUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
            if (!storedToken.getTokenHash().equals(hashToken(refreshTokenValue))) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }
            refreshTokenRepository.delete(storedToken);

            String bearerToken = httpRequest.getHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            return null;
        } catch (BusinessException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String phoneNumber = phoneVerificationService.consumeVerified(request.phoneNumber());
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        user.resetLoginFail();
        refreshTokenRepository.findByUserId(user.getId())
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public Map<String, Object> kakaoLogin(String kakaoAccessToken) {
        Map<String, Object> kakaoUserInfo = kakaoClient.getUserInfo(kakaoAccessToken);
        Object idObj = kakaoUserInfo.get("id");
        if (idObj == null) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }
        String kakaoId = String.valueOf(idObj);

        return userRepository.findByKakaoId(kakaoId)
                .map(user -> {
                    if (user.isDeleted()) {
                        throw new BusinessException(ErrorCode.WITHDRAWN_USER);
                    }
                    TokenResponse token = issueAndStoreTokens(user);
                    return Map.<String, Object>of("isNewUser", false, "token", token);
                })
                .orElseGet(() -> Map.of("isNewUser", true, "kakaoId", kakaoId));
    }

    @Transactional
    public TokenResponse kakaoSignup(KakaoSignupRequest request) {
        Map<String, Object> kakaoUserInfo = kakaoClient.getUserInfo(request.kakaoAccessToken());
        Object idObj = kakaoUserInfo.get("id");
        if (idObj == null) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }
        String kakaoId = String.valueOf(idObj);

        String phoneNumber = phoneVerificationService.consumeVerified(request.phoneNumber());

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> userRepository.save(User.ofKakao(kakaoId, phoneNumber, request.name())));

        if (user.getKakaoId() != null && !user.getKakaoId().equals(kakaoId)) {
            throw new BusinessException(ErrorCode.KAKAO_ALREADY_LINKED);
        }

        user.linkKakao(kakaoId);
        if (user.getName() == null) {
            user.updateName(request.name());
        }

        return issueAndStoreTokens(user);
    }

    @Transactional
    public void unlock(UnlockRequest request) {
        String phoneNumber = phoneVerificationService.consumeVerified(request.phoneNumber());
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        user.resetLoginFail();
    }

    private TokenResponse issueAndStoreTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshTokenValue = jwtUtil.generateRefreshToken(user.getId(), user.getRole());
        Instant expiresAt = jwtUtil.getExpiration(refreshTokenValue).toInstant();
        String tokenHash = hashToken(refreshTokenValue);

        RefreshToken storedToken = refreshTokenRepository.findByUserId(user.getId())
                .orElseGet(() -> new RefreshToken(user.getId(), tokenHash, expiresAt));
        storedToken.rotate(tokenHash, expiresAt);
        refreshTokenRepository.save(storedToken);

        return new TokenResponse(
                "Bearer",
                accessToken,
                refreshTokenValue,
                jwtUtil.getAccessTokenExpirationSeconds()
        );
    }

    /*
    DB가 노출되더라도 저장된 값만으로 Refresh Token 원문을
    바로 사용할 수 없도록 하기 위한 처리 - hashToken
     */

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replace("-", "");
    }
}
