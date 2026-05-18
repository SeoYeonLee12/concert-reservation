package com.example.concertreservation.reservation.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentKafkaConsumer {

    /**
     * payment.confirmed 토픽 소비.
     * 현재는 티켓 발행·이메일 알림을 로그로 시뮬레이션.
     * 실제 운영에서는 외부 알림 서비스 호출로 교체.
     */
    @KafkaListener(
            topics = PaymentEventListener.TOPIC,
            groupId = "concert-reservation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info("[Kafka 수신] topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        log.info("[티켓 발행] reservationId={}, payload={}", record.key(), record.value());
        log.info("[이메일 알림] reservationId={} — 예약 확정 안내 발송 완료", record.key());
    }
}
