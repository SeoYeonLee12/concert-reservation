package com.example.concertreservation.reservation.application.strategy;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.reservation.application.ReservationTransactionalService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component("redisson")
@RequiredArgsConstructor
public class RedissonReservationStrategy implements ReservationLockStrategy {

    private static final String SEAT_LOCK_KEY_PREFIX = "lock:seat:";
    private static final long LOCK_WAIT_TIME_MS = 3_000L;
    private static final long LOCK_LEASE_TIME_MS = 5_000L;

    private final RedissonClient redissonClient;
    private final ReservationTransactionalService reservationTransactionalService;

    @Override
    public Long tryReserve(Long userId, Long performanceSeatId) {
        RLock lock = redissonClient.getLock(SEAT_LOCK_KEY_PREFIX + performanceSeatId);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_MS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new GlobalException(ReservationErrorCode.SEAT_LOCK_TIMEOUT);
            }
            return reservationTransactionalService.doReserve(userId, performanceSeatId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GlobalException(ReservationErrorCode.SEAT_LOCK_INTERRUPTED);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
