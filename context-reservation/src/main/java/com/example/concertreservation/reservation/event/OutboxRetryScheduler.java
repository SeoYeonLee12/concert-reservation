package com.example.concertreservation.reservation.event;

import com.example.concertreservation.global.outbox.OutboxEvent;
import com.example.concertreservation.global.outbox.OutboxEventRepository;
import com.example.concertreservation.global.outbox.OutboxStatus;
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

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 30초마다 PENDING 상태의 outbox 이벤트를 Kafka에 재발행.
     * PaymentEventListener에서 발행에 실패한 이벤트를 at-least-once로 복구.
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void retryPendingEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
        List<OutboxEvent> stuckEvents =
                outboxEventRepository.findByStatusAndCreatedAtBefore(OutboxStatus.PENDING, threshold);

        if (stuckEvents.isEmpty()) {
            return;
        }

        log.warn("[Outbox 재처리] PENDING 이벤트 {}건 Kafka 재발행 시도", stuckEvents.size());
        stuckEvents.forEach(e -> {
            try {
                kafkaTemplate.send(
                        PaymentEventListener.TOPIC,
                        String.valueOf(e.getAggregateId()),
                        e.getPayload()
                );
                e.markPublished();
                log.info("[Outbox 재처리 완료] id={}, aggregateId={}", e.getId(), e.getAggregateId());
            } catch (Exception ex) {
                log.error("[Outbox 재처리 실패] id={}: {}", e.getId(), ex.getMessage());
                e.markFailed();
            }
        });
    }
}
