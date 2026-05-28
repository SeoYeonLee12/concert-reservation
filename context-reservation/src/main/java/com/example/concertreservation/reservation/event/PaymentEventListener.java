package com.example.concertreservation.reservation.event;

import com.example.concertreservation.global.event.DomainEventRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
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

    private final DomainEventRepository domainEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 결제 트랜잭션 커밋 이후 비동기로 Kafka에 이벤트 발행.
     * message key = DomainEvent.uuid → 컨슈머 Redis 멱등성 검사에 사용.
     * 발행 성공 → PRODUCE_SUCCESS, 실패 → PRODUCE_FAIL (OutboxRetryScheduler 재처리 없음).
     * INIT 상태로 남은 이벤트는 OutboxRetryScheduler가 30초 후 재처리.
     */
    @Async("EVENT_ASYNC_TASK_EXECUTOR")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        domainEventRepository.findById(event.domainEventId()).ifPresent(domainEvent -> {
            PaymentConfirmedDomainEvent paymentEvent = (PaymentConfirmedDomainEvent) domainEvent;
            try {
                kafkaTemplate.send(TOPIC, paymentEvent.getUuid(), paymentEvent.getPayload())
                        .get(10, TimeUnit.SECONDS);
                paymentEvent.produceSuccess();
                log.info("[Kafka 발행 완료] domainEventId={}, uuid={}, reservationId={}",
                        paymentEvent.getId(), paymentEvent.getUuid(), event.reservationId());
            } catch (Exception e) {
                paymentEvent.produceFail(e);
                log.error("[Kafka 발행 실패] domainEventId={}, uuid={}: {}",
                        paymentEvent.getId(), paymentEvent.getUuid(), e.getMessage());
            }
            domainEventRepository.save(paymentEvent);
        });
    }
}
