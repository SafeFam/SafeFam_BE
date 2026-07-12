package com.gold.safefam.global.security;

/**
 * Access Token과 Refresh Token을 명확히 구분
 * Refresh Token을 보호  API 인증에 사용하는 것 차단
 * Access Token을 재발급 API에 사용하는 것 차단
 */

public enum TokenType {
    ACCESS,
    REFRESH
}
