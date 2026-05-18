package com.example.concertreservation.reservation.event;

import com.example.concertreservation.global.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * 결제 트랜잭션 커밋 이후 실행. REQUIRES_NEW로 새 트랜잭션에서 OutboxEvent 상태 갱신.
     * Day 4에서 Kafka Publisher로 대체 예정.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("[티켓 발행] reservationId={}", event.reservationId());
        log.info("[이메일 발송] userId={}, price={}원", event.userId(), event.price());

        outboxEventRepository.findById(event.outboxEventId())
                .ifPresent(outboxEvent -> {
                    outboxEvent.markPublished();
                    outboxEventRepository.save(outboxEvent);
                });
    }
}
