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
     * uuid를 Redis에 SETNX로 기록. 이미 처리된 uuid면 true 반환 (중복).
     */
    public boolean isDuplicate(String uuid) {
        String key = IDEMPOTENT_KEY_PREFIX + uuid;
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
        return Boolean.FALSE.equals(isNew);
    }
}
