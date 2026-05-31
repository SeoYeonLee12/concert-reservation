package com.example.concertreservation.global.kafka.consumer;

import com.example.concertreservation.global.kafka.producer.PaymentEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final KafkaIdempotencyChecker idempotencyChecker;

    @KafkaListener(
            topics = PaymentEventListener.TOPIC,
            groupId = "concert-reservation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String uuid = record.key();

        if (idempotencyChecker.isDuplicate(uuid)) {
            log.warn("[Kafka 중복 수신 무시] uuid={}, offset={}", uuid, record.offset());
            return;
        }

        log.info("[Kafka 수신] topic={}, partition={}, offset={}, uuid={}",
                record.topic(), record.partition(), record.offset(), uuid);
        log.info("[티켓 발행] uuid={}, payload={}", uuid, record.value());
        log.info("[이메일 알림] uuid={} — 예약 확정 안내 발송 완료", uuid);
    }
}
