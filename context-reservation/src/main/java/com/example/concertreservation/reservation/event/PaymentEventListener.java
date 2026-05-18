package com.example.concertreservation.reservation.event;

import com.example.concertreservation.global.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    static final String TOPIC = "payment.confirmed";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 결제 트랜잭션 커밋 이후 Kafka에 이벤트 발행.
     * 발행 성공 시 OutboxEvent → PUBLISHED, 실패 시 PENDING 유지 (OutboxRetryScheduler가 재처리).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        outboxEventRepository.findById(event.outboxEventId()).ifPresent(outboxEvent -> {
            try {
                kafkaTemplate.send(TOPIC, String.valueOf(event.reservationId()), outboxEvent.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("[Kafka 발행 실패] outboxId={}, reservationId={}: {}",
                                        outboxEvent.getId(), event.reservationId(), ex.getMessage());
                            } else {
                                log.info("[Kafka 발행 완료] outboxId={}, reservationId={}, partition={}, offset={}",
                                        outboxEvent.getId(), event.reservationId(),
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            }
                        });
                outboxEvent.markPublished();
                outboxEventRepository.save(outboxEvent);
            } catch (Exception e) {
                log.error("[Kafka 발행 예외] outboxId={}: {}", outboxEvent.getId(), e.getMessage());
            }
        });
    }
}
