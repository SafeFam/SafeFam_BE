package com.gold.safefam.global.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String[] RATE_LIMITED_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/phone-verifications/send"
    };

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastAccess = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public RateLimitFilter() {
        // 10분마다 5분 이상 미접근 IP 제거
        scheduler.scheduleAtFixedRate(this::evictExpiredBuckets, 10, 10, TimeUnit.MINUTES);
    }

    private void evictExpiredBuckets() {
        long now = System.currentTimeMillis();
        lastAccess.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > TimeUnit.MINUTES.toMillis(5)) {
                buckets.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    private Bucket getBucket(String ip) {
        lastAccess.put(ip, System.currentTimeMillis());
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
        String method = request.getMethod();
        boolean isRateLimited = false;

        if (HttpMethod.POST.matches(method)) {
            for (String limited : RATE_LIMITED_PATHS) {
                if (path.equals(limited)) {
                    isRateLimited = true;
                    break;
                }
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

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
    }
}
