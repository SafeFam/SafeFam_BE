package com.gold.safefam.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "blacklist:";

    public void blacklist(String token, long remainingMillis) {
        if (remainingMillis > 0) {
            redisTemplate.opsForValue()
                    .set(PREFIX + token, "1", remainingMillis, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}