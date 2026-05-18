package com.example.concertreservation.reservation.application;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final String SEAT_LOCK_KEY_PREFIX = "lock:seat:";
    private static final long LOCK_WAIT_TIME_MS = 3_000L;
    private static final long LOCK_LEASE_TIME_MS = 5_000L;

    private final ReservationTransactionalService reservationTransactionalService;
    private final RedissonClient redissonClient;

    /**
     * RES-01 좌석 선점.
     * 락을 @Transactional 경계 밖에서 관리한다.
     * doReserve() 반환 시점 = 트랜잭션 커밋 완료이므로, finally의 unlock()은 커밋 이후에 실행된다.
     * 이로써 "락 해제 → 커밋 전 다른 스레드 진입" 레이스 컨디션을 제거한다.
     */
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

    public void confirmPayment(Long userId, Long reservationId) {
        reservationTransactionalService.doConfirmPayment(userId, reservationId);
    }

    public void cancelReservation(Long userId, Long reservationId) {
        reservationTransactionalService.doCancelReservation(userId, reservationId);
    }
}
