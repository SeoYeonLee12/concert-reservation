package com.example.concertreservation.reservation.application.strategy;

public interface ReservationLockStrategy {
    Long tryReserve(Long userId, Long performanceSeatId);
}
