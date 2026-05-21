package com.example.concertreservation.reservation.application;

import com.example.concertreservation.reservation.application.strategy.ReservationLockStrategy;
import com.example.concertreservation.waiting.application.WaitingQueueService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final String DEFAULT_STRATEGY = "redisson";

    private final Map<String, ReservationLockStrategy> strategies;
    private final ReservationTransactionalService reservationTransactionalService;
    private final WaitingQueueService waitingQueueService;

    public Long tryReserve(Long userId, Long performanceSeatId, String strategy) {
        ReservationLockStrategy lockStrategy = strategies.getOrDefault(strategy, strategies.get(DEFAULT_STRATEGY));
        log.debug("[좌석 선점] strategy={}, userId={}, seatId={}", strategy, userId, performanceSeatId);

        Long reservationId = lockStrategy.tryReserve(userId, performanceSeatId);
        waitingQueueService.markDone(userId, performanceSeatId);
        return reservationId;
    }

    public void confirmPayment(Long userId, Long reservationId) {
        reservationTransactionalService.doConfirmPayment(userId, reservationId);
    }

    public void cancelReservation(Long userId, Long reservationId) {
        reservationTransactionalService.doCancelReservation(userId, reservationId);
    }
}
