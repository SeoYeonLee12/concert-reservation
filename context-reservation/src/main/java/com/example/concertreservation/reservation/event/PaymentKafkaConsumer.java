package com.example.concertreservation.reservation.event;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private static final String IDEMPOTENT_KEY_PREFIX = "kafka:idempotent:payment:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    @KafkaListener(
            topics = PaymentEventListener.TOPIC,
            groupId = "concert-reservation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String uuid = record.key();
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + uuid;

        Boolean isNew = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL);

        if (Boolean.FALSE.equals(isNew)) {
            log.warn("[Kafka 중복 수신 무시] uuid={}, offset={}", uuid, record.offset());
            return;
        }

        log.info("[Kafka 수신] topic={}, partition={}, offset={}, uuid={}",
                record.topic(), record.partition(), record.offset(), uuid);
        log.info("[티켓 발행] uuid={}, payload={}", uuid, record.value());
        log.info("[이메일 알림] uuid={} — 예약 확정 안내 발송 완료", uuid);
    }
}
