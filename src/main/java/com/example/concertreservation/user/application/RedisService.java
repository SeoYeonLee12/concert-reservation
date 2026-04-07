package com.example.concertreservation.user.application;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final String REDIS_KEY_PREFIX = "RT:";
    private final StringRedisTemplate stringRedisTemplate;

    public void save(Long userId, String refreshToken, long expirationMillis) {
        String key = REDIS_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(
                key,
                refreshToken,
                Duration.ofMillis(expirationMillis)
        );
    }

    public String getRefreshToken(Long userId) {
        return stringRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + userId);
    }

    public void delete(Long userId) {
        stringRedisTemplate.delete(REDIS_KEY_PREFIX + userId);
    }
}
