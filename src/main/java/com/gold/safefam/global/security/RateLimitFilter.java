package com.gold.safefam.global.security;

import com.gold.safefam.global.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String[] IP_RATE_LIMITED_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/phone-verifications/send"
    };

    private static final String[] USER_RATE_LIMITED_PATHS = {
            "/api/v1/analyses"
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

    private Bucket getBucket(String key) {
        lastAccess.put(key, System.currentTimeMillis());
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
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
        String bucketKey = null;

        if (HttpMethod.POST.matches(method)) {
            for (String limited : IP_RATE_LIMITED_PATHS) {
                if (path.equals(limited)) {
                    bucketKey = "ip:" + request.getRemoteAddr();
                    break;
                }
            }

            if (bucketKey == null) {
                for (String limited : USER_RATE_LIMITED_PATHS) {
                    if (path.equals(limited)) {
                        bucketKey = "user:" + resolveUserId();
                        break;
                    }
                }
            }
        }

        if (bucketKey != null) {
            Bucket bucket = getBucket(bucketKey);
            if (!bucket.tryConsume(1)) {
                ErrorCode errorCode = bucketKey.startsWith("user:")
                        ? ErrorCode.ANALYSIS_RATE_LIMIT_EXCEEDED
                        : ErrorCode.RATE_LIMIT_EXCEEDED;
                writeRateLimitResponse(response, errorCode);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitResponse(HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":\"ERROR\",\"message\":\"" + errorCode.getMessage() + "\",\"data\":null}"
        );
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof Long userId) {
            return String.valueOf(userId);
        }
        return "anonymous";
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
    }
}
