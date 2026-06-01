package com.example.concertreservation.global.kafka.consumer;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaIdempotencyChecker {

    private static final String IDEMPOTENT_KEY_PREFIX = "kafka:idempotent:payment:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * uuid가 이미 처리되었는지 확인 (Redis 키 존재 여부).
     */
    public boolean isAlreadyProcessed(String uuid) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(IDEMPOTENT_KEY_PREFIX + uuid));
    }

    /**
     * uuid를 처리됨으로 표시 (Redis에 기록).
     */
    public void markAsProcessed(String uuid) {
        stringRedisTemplate.opsForValue().set(IDEMPOTENT_KEY_PREFIX + uuid, "1", IDEMPOTENT_TTL);
    }
}
