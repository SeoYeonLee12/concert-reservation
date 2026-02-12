package com.example.concertreservation.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final String REDIS_KEY_PREFIX = "RT:";
    private final RedisTemplate<String, Object> redisTemplate;

    public void save(Long userId, String refreshToken, long expirationMillis) {
        String key = REDIS_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                java.time.Duration.ofMillis(expirationMillis)
        );
    }

    public String getRefreshToken(Long userId) {
        return (String) redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + userId);
    }

    public void delete(Long userId) {
        redisTemplate.delete(REDIS_KEY_PREFIX + userId);
    }
}
