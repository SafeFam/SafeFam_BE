package com.gold.safefam.global.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final String[] RATE_LIMITED_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/phone-verifications/send"
    };

    private Bucket getBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillIntervally(10, Duration.ofMinutes(1))
                        .build())
                .build());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isRateLimited = false;
        for (String limited : RATE_LIMITED_PATHS) {
            if (path.equals(limited)) {
                isRateLimited = true;
                break;
            }
        }

        if (isRateLimited) {
            String ip = request.getRemoteAddr();
            Bucket bucket = getBucket(ip);
            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests. Please try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}