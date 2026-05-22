package com.example.concertreservation.reservation.application.strategy;

import com.example.concertreservation.global.aop.retry.Retry;
import com.example.concertreservation.reservation.application.ReservationTransactionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
