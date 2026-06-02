package com.example.concertreservation.reservation.application.strategy;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.reservation.application.ReservationTransactionalService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MySQL Named Lock 기반 좌석 선점 전략.
 *
 * 개선 A — 별도 DataSource:
 *   NamedLockService가 namedLockJdbcTemplate(전용 풀)을 사용.
 *   GET_LOCK이 메인 풀을 소모하지 않아 pool=10 기본값에서도 동작.
 *
 * 개선 B — Semaphore 동시 진입 제한:
 *   permits=4 → 동시 Named Lock 진입 스레드 최대 4개.
 *   최대 연결 소모 = 4(Named Lock 풀) + 4(메인 풀) = 8 < pool=10 → 수학적 고갈 불가.
 *   fair=true: FIFO 순서로 대기 스레드 처리, 기아 방지.
 */
@Slf4j
@Component("named-lock")
public class NamedLockReservationStrategy implements ReservationLockStrategy {

    private static final String LOCK_KEY_PREFIX = "seat:";
    private static final int SEMAPHORE_PERMITS = 4;
    private static final long SEMAPHORE_TIMEOUT_SEC = 10L;

    private final Semaphore semaphore;
    private final NamedLockService namedLockService;
    private final ReservationTransactionalService reservationTransactionalService;

    public NamedLockReservationStrategy(NamedLockService namedLockService,
            ReservationTransactionalService reservationTransactionalService) {
        this.namedLockService = namedLockService;
        this.reservationTransactionalService = reservationTransactionalService;
        this.semaphore = new Semaphore(SEMAPHORE_PERMITS, true);
    }

    @Override
    public Long tryReserve(Long userId, Long performanceSeatId) {
        String lockKey = LOCK_KEY_PREFIX + performanceSeatId;

        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(SEMAPHORE_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GlobalException(ReservationErrorCode.SEAT_LOCK_TIMEOUT);
        }
        if (!acquired) {
            log.warn("[Semaphore] 진입 허가 타임아웃: key={}", lockKey);
            throw new GlobalException(ReservationErrorCode.SEAT_LOCK_TIMEOUT);
        }

        try {
            namedLockService.getLock(lockKey);
            return reservationTransactionalService.doReserve(userId, performanceSeatId);
        } finally {
            namedLockService.releaseLock(lockKey);
            semaphore.release();
        }
    }
}
