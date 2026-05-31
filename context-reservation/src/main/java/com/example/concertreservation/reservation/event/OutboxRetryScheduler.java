package com.example.concertreservation.reservation.event;

import com.example.concertreservation.global.event.DomainEvent;
import com.example.concertreservation.global.event.DomainEventRepository;
import com.example.concertreservation.global.event.EventStatus;
import com.example.concertreservation.global.kafka.deadletter.DeadLetter;
import com.example.concertreservation.global.kafka.deadletter.DeadLetterRepository;
import com.example.concertreservation.global.kafka.producer.PaymentConfirmedDomainEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRetryScheduler {

    static final int MAX_RETRY = 5;

    private final DomainEventRepository domainEventRepository;
    private final DeadLetterRepository deadLetterRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 30초마다 INIT·PRODUCE_FAIL 상태의 DomainEvent를 재발행.
     * retryCount >= MAX_RETRY 초과 시 ABANDONED 처리 후 DeadLetter 기록.
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void retryPendingEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
        List<DomainEvent> retryTargets = domainEventRepository.findByStatusInAndCreatedAtBefore(
                List.of(EventStatus.INIT, EventStatus.PRODUCE_FAIL), threshold);

        if (retryTargets.isEmpty()) {
            return;
        }

        log.warn("[Outbox 재처리] 대상 {}건 (INIT/PRODUCE_FAIL)", retryTargets.size());
        retryTargets.forEach(this::retryOrAbandon);
    }

    private void retryOrAbandon(DomainEvent event) {
        if (!(event instanceof PaymentConfirmedDomainEvent paymentEvent)) {
            return;
        }

        if (paymentEvent.getRetryCount() >= MAX_RETRY) {
            abandonEvent(paymentEvent);
            return;
        }

        paymentEvent.increaseRetryCount();
        try {
            kafkaTemplate.send(paymentEvent.getTopic(), paymentEvent.getUuid(), paymentEvent.getPayload())
                    .get(10, TimeUnit.SECONDS);
            paymentEvent.produceSuccess();
            log.info("[Outbox 재처리 완료] id={}, uuid={}, retryCount={}",
                    paymentEvent.getId(), paymentEvent.getUuid(), paymentEvent.getRetryCount());
        } catch (Exception ex) {
            paymentEvent.produceFail(ex);
            log.error("[Outbox 재처리 실패] id={}, retryCount={}: {}",
                    paymentEvent.getId(), paymentEvent.getRetryCount(), ex.getMessage());
        }
    }

    private void abandonEvent(PaymentConfirmedDomainEvent paymentEvent) {
        paymentEvent.abandon();
        deadLetterRepository.save(new DeadLetter(
                paymentEvent.getUuid(),
                paymentEvent.getId(),
                paymentEvent.getFailReason()
        ));
        log.warn("[Outbox 포기] id={}, uuid={}, retryCount={} → ABANDONED + DeadLetter 기록",
                paymentEvent.getId(), paymentEvent.getUuid(), paymentEvent.getRetryCount());
    }
}
