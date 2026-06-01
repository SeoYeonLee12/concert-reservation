package com.example.concertreservation.global.kafka.consumer;

import com.example.concertreservation.global.kafka.producer.PaymentEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final KafkaIdempotencyChecker idempotencyChecker;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2_000, multiplier = 2.0, maxDelay = 30_000),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = PaymentEventListener.TOPIC,
            groupId = "concert-reservation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String uuid = record.key();

        if (idempotencyChecker.isAlreadyProcessed(uuid)) {
            log.warn("[Kafka 중복 수신 무시] uuid={}, offset={}", uuid, record.offset());
            return;
        }

        log.info("[Kafka 수신] topic={}, partition={}, offset={}, uuid={}",
                record.topic(), record.partition(), record.offset(), uuid);
        log.info("[티켓 발행] uuid={}, payload={}", uuid, record.value());
        log.info("[이메일 알림] uuid={} — 예약 확정 안내 발송 완료", uuid);

        // 처리 성공 후 멱등성 키 설정 — @RetryableTopic 재시도와 충돌 방지
        idempotencyChecker.markAsProcessed(uuid);
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        log.error("[DLT 수신 - 최대 재시도 초과] uuid={}, topic={}, payload={}",
                record.key(), record.topic(), record.value());
    }
}
