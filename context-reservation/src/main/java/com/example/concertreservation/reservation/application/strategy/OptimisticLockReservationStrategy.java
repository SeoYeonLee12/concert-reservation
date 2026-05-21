package com.example.concertreservation.reservation.application.strategy;

import com.example.concertreservation.global.aop.retry.Retry;
import com.example.concertreservation.reservation.application.ReservationTransactionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JPA 낙관적 락(@Version) 기반 좌석 선점 전략.
 * PerformanceSeat.version 필드로 충돌을 감지하고,
 * OptimisticLockRetryAspect가 ObjectOptimisticLockingFailureException 발생 시 재시도한다.
 * 재시도 초과 시 GlobalExceptionHandler가 SEAT_CONFLICT(409)로 응답한다.
 */
@Slf4j
@Component("optimistic")
@RequiredArgsConstructor
public class OptimisticLockReservationStrategy implements ReservationLockStrategy {

    private final ReservationTransactionalService reservationTransactionalService;

    @Retry(maxRetries = 10, retryDelay = 50)
    @Override
    public Long tryReserve(Long userId, Long performanceSeatId) {
        return reservationTransactionalService.doReserve(userId, performanceSeatId);
    }
}
