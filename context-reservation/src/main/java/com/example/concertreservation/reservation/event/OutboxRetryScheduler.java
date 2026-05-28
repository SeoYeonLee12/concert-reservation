package com.example.concertreservation.reservation.event;

import com.example.concertreservation.global.event.DomainEvent;
import com.example.concertreservation.global.event.DomainEventRepository;
import com.example.concertreservation.global.event.EventStatus;
import java.time.LocalDateTime;
import java.util.List;
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

    private final DomainEventRepository domainEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 30초마다 INIT 상태의 DomainEvent를 Kafka에 재발행.
     * PaymentEventListener 비동기 발행 실패 시 at-least-once 복구.
     * message key = uuid → 컨슈머 Redis 멱등성으로 중복 처리 방지.
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void retryPendingEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
        List<DomainEvent> stuckEvents =
                domainEventRepository.findByStatusAndCreatedAtBefore(EventStatus.INIT, threshold);

        if (stuckEvents.isEmpty()) {
            return;
        }

        log.warn("[Outbox 재처리] INIT 이벤트 {}건 Kafka 재발행 시도", stuckEvents.size());
        stuckEvents.forEach(e -> {
            if (e instanceof PaymentConfirmedDomainEvent paymentEvent) {
                try {
                    kafkaTemplate.send(
                            paymentEvent.getTopic(),
                            paymentEvent.getUuid(),
                            paymentEvent.getPayload()
                    );
                    paymentEvent.produceSuccess();
                    log.info("[Outbox 재처리 완료] id={}, uuid={}", paymentEvent.getId(), paymentEvent.getUuid());
                } catch (Exception ex) {
                    paymentEvent.produceFail(ex);
                    log.error("[Outbox 재처리 실패] id={}: {}", paymentEvent.getId(), ex.getMessage());
                }
            }
        });
    }
}
