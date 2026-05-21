package com.example.concertreservation.reservation.application.strategy;

import com.example.concertreservation.reservation.application.ReservationTransactionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MySQL Named Lock 기반 좌석 선점 전략.
 * GET_LOCK/RELEASE_LOCK은 동일 커넥션에서 실행된다.
 * 락을 @Transactional 경계 밖에서 획득·해제하여 커밋 후 락 해제를 보장한다.
 */
@Slf4j
@Component("named-lock")
@RequiredArgsConstructor
public class NamedLockReservationStrategy implements ReservationLockStrategy {

    private static final String LOCK_KEY_PREFIX = "seat:";

    private final NamedLockService namedLockService;
    private final ReservationTransactionalService reservationTransactionalService;

    @Override
    public Long tryReserve(Long userId, Long performanceSeatId) {
        String lockKey = LOCK_KEY_PREFIX + performanceSeatId;
        try {
            namedLockService.getLock(lockKey);
            return reservationTransactionalService.doReserve(userId, performanceSeatId);
        } finally {
            namedLockService.releaseLock(lockKey);
        }
    }
}
