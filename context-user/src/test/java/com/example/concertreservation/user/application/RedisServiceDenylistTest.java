package com.example.concertreservation.user.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisServiceDenylistTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisService redisService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        redisService = new RedisService(stringRedisTemplate);
    }

    @Test
    void blacklistAccessToken_올바른키로_저장() {
        String jti = "test-jti-uuid";
        long ttl = 3_600_000L;

        redisService.blacklistAccessToken(jti, ttl);

        verify(valueOperations).set("BL:" + jti, "1", Duration.ofMillis(ttl));
    }

    @Test
    void isBlacklisted_키_존재시_true() {
        String jti = "test-jti-uuid";
        when(stringRedisTemplate.hasKey("BL:" + jti)).thenReturn(Boolean.TRUE);

        assertThat(redisService.isBlacklisted(jti)).isTrue();
    }

    @Test
    void isBlacklisted_키_없을시_false() {
        String jti = "unknown-jti";
        when(stringRedisTemplate.hasKey("BL:" + jti)).thenReturn(Boolean.FALSE);

        assertThat(redisService.isBlacklisted(jti)).isFalse();
    }
}
