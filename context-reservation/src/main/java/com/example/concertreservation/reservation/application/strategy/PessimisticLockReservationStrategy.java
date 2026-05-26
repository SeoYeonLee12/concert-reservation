package com.example.concertreservation.reservation.application.strategy;

import com.example.concertreservation.reservation.application.ReservationTransactionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// DB 비관적 락(SELECT FOR UPDATE). 트랜잭션 내에서 잠금 획득 — Redisson과 달리 락-트랜잭션 경계가 일치해 별도 경계 분리 불필요.
@Component("pessimistic")
@RequiredArgsConstructor
public class PessimisticLockReservationStrategy implements ReservationLockStrategy {

    private final ReservationTransactionalService reservationTransactionalService;

    @Override
    public Long tryReserve(Long userId, Long performanceSeatId) {
        return reservationTransactionalService.doReserveWithPessimisticLock(userId, performanceSeatId);
    }
}
