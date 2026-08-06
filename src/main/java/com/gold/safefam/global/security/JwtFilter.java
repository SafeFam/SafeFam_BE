package com.gold.safefam.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 모든 HTTP 요청에서 JWT 인증을 처리하는 필터
 *
 * Authorization: Bearer ...에서 토큰 추출
 * 서명 및 만료 검증
 * ACCESS 토큰만 인증에 사용
 * 토큰의 사용자 ID를 principal로 등록
 * 토큰의 역할을 ROLE_USER 또는 ROLE_ADMIN 권한으로 변환
 * 인증 객체를 SecurityContextHolder에 등록
 * Claim이 잘못된 토큰은 인증 처리하지 않고 Context 초기화
 *
 *
 * Refresh Token을 Authorization 헤더에 넣어도
 * 로그인된 것으로 처리되지 않음!
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            try {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                // Redis 장애 시 블랙리스트 체크 생략 (fail-open)
                // 가용성 우선: Redis 다운되어도 정상 요청은 통과시킴
                log.warn("Redis blacklist check failed, fail-open: {}", e.getMessage());
            }
            try {
                if (jwtUtil.getTokenType(token) == TokenType.ACCESS) {
                    Long userId = jwtUtil.getUserId(token);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    List.of(new SimpleGrantedAuthority(
                                            "ROLE_" + jwtUtil.getRole(token).name()))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
