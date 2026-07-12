package com.gold.safefam.global.security;

import com.gold.safefam.domain.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 생성, 파싱, 검증을 담당하는 파일
 */


@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(Long userId) {
        return generateAccessToken(userId, UserRole.USER);
    }

    public String generateAccessToken(Long userId, UserRole role) {
        return generateToken(userId, role, TokenType.ACCESS, accessTokenExpiration);
    }

    public String generateRefreshToken(Long userId, UserRole role) {
        return generateToken(userId, role, TokenType.REFRESH, refreshTokenExpiration);
    }

    private String generateToken(Long userId, UserRole role, TokenType tokenType, long expiration) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .claim("tokenType", tokenType.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public UserRole getRole(String token) {
        return UserRole.valueOf(parseClaims(token).get("role", String.class));
    }

    public TokenType getTokenType(String token) {
        return TokenType.valueOf(parseClaims(token).get("tokenType", String.class));
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    public void validateTokenType(String token, TokenType expectedType) {
        Claims claims = parseClaims(token);
        if (!expectedType.name().equals(claims.get("tokenType", String.class))) {
            throw new JwtException("Invalid token type");
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
