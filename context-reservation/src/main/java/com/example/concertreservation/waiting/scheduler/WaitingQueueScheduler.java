package com.example.concertreservation.waiting.scheduler;

import com.example.concertreservation.waiting.domain.WaitingQueue;
import com.example.concertreservation.waiting.domain.WaitingQueueRepository;
import com.example.concertreservation.waiting.domain.enums.WaitingQueueStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingQueueScheduler {

    private static final int ACTIVATE_BATCH_SIZE = 5;
    private static final Duration ACTIVE_EXPIRY_WINDOW = Duration.ofMinutes(5);

    private final WaitingQueueRepository waitingQueueRepository;

    /**
     * 10초마다 WAITING → ACTIVE 전환 (상위 N명).
     */
    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void activateWaiting() {
        List<WaitingQueue> waiting = waitingQueueRepository
                .findAllByStatusOrderByQueuePositionAsc(WaitingQueueStatus.WAITING);

        waiting.stream()
                .limit(ACTIVATE_BATCH_SIZE)
                .forEach(wq -> {
                    wq.activate();
                    log.info("[대기열 활성화] waitingQueueId={}, userId={}, seatId={}, position={}",
                            wq.getWaitingQueueId(), wq.getUserId(),
                            wq.getPerformanceSeatId(), wq.getQueuePosition());
                });
    }

    /**
     * 30초마다 ACTIVE 상태 5분 경과 항목 → EXPIRED 처리.
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void expireActiveQueues() {
        LocalDateTime expiredBefore = LocalDateTime.now().minus(ACTIVE_EXPIRY_WINDOW);
        List<WaitingQueue> expiredQueues = waitingQueueRepository
                .findAllByStatusAndActivatedAtBefore(WaitingQueueStatus.ACTIVE, expiredBefore);

        expiredQueues.forEach(wq -> {
            wq.expire();
            log.info("[대기열 만료] waitingQueueId={}, userId={}, seatId={}",
                    wq.getWaitingQueueId(), wq.getUserId(), wq.getPerformanceSeatId());
        });
    }
}
