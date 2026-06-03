package com.example.concertreservation.reservation.application;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationAsyncService {

    public static final String STATUS_KEY_PREFIX = "confirm:status:";
    static final long STATUS_TTL_MINUTES = 10;

    private final ReservationTransactionalService reservationTransactionalService;
    private final StringRedisTemplate stringRedisTemplate;

    @Async("EVENT_ASYNC_TASK_EXECUTOR")
    public void confirmAsync(Long userId, Long reservationId) {
        try {
            reservationTransactionalService.doConfirmPayment(userId, reservationId);
            setStatus(reservationId, "COMPLETED");
            log.info("[결제 확정 완료] reservationId={}", reservationId);
        } catch (Exception e) {
            setStatus(reservationId, "FAILED");
            log.error("[결제 확정 실패] reservationId={} error={}", reservationId, e.getMessage());
        }
    }

    private void setStatus(Long reservationId, String status) {
        stringRedisTemplate.opsForValue()
                .set(STATUS_KEY_PREFIX + reservationId, status, STATUS_TTL_MINUTES, TimeUnit.MINUTES);
    }
}
