package com.example.concertreservation.reservation.event;

import com.example.concertreservation.global.outbox.OutboxEventRepository;
import com.example.concertreservation.global.outbox.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.example.concertreservation.global.outbox.OutboxEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRetryScheduler {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * 커밋 후 리스너 처리에 실패한 PENDING 이벤트를 주기적으로 재처리.
     * Day 4에서 Kafka re-publish 로직으로 교체.
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

        log.warn("[Outbox 재처리] PENDING 이벤트 {}건 재시도", stuckEvents.size());
        stuckEvents.forEach(e -> {
            log.warn("[Outbox 재처리] id={}, eventType={}, aggregateId={}",
                    e.getId(), e.getEventType(), e.getAggregateId());
            e.markPublished();
        });
    }
}
