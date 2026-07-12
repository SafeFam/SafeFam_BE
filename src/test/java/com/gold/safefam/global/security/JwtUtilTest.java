package com.gold.safefam.global.security;

import com.gold.safefam.domain.user.enums.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET =
            "c2FmZWZhbS1zZWNyZXQta2V5LWZvci1qd3QtYXV0aGVudGljYXRpb24=";

    @Test
    void accessTokenContainsIdentityRoleAndType() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000);

        String token = jwtUtil.generateAccessToken(7L, UserRole.ADMIN);

        assertTrue(jwtUtil.validateToken(token));
        assertEquals(7L, jwtUtil.getUserId(token));
        assertEquals(UserRole.ADMIN, jwtUtil.getRole(token));
        assertEquals(TokenType.ACCESS, jwtUtil.getTokenType(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000);
        String token = jwtUtil.generateAccessToken(1L, UserRole.USER);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1_000, 120_000);
        String token = jwtUtil.generateAccessToken(1L, UserRole.USER);

        assertFalse(jwtUtil.validateToken(token));
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.parseClaims(token));
    }

    @Test
    void refreshTokenCannotBeValidatedAsAccessToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000);
        String refreshToken = jwtUtil.generateRefreshToken(1L, UserRole.USER);

        assertThrows(Exception.class,
                () -> jwtUtil.validateTokenType(refreshToken, TokenType.ACCESS));
    }
}
