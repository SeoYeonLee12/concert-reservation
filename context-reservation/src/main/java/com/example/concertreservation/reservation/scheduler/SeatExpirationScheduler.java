package com.example.concertreservation.reservation.scheduler;

import com.example.concertreservation.performanceseat.domain.PerformanceSeat;
import com.example.concertreservation.performanceseat.domain.PerformanceSeatRepository;
import com.example.concertreservation.reservation.domain.ReservationRepository;
import com.example.concertreservation.reservation.domain.enums.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatExpirationScheduler {

    private static final long EXPIRY_MINUTES = 5L;

    private final PerformanceSeatRepository performanceSeatRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 5분 이상 TEMPORARY 상태인 좌석을 해제하고 연관된 PENDING 예약을 EXPIRED 처리.
     * 1분마다 실행 (결제 미완료 좌석이 최대 6분간 점유되도록 허용).
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireSeats() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(EXPIRY_MINUTES);
        List<PerformanceSeat> expiredSeats =
                performanceSeatRepository.findExpiredTemporarySeats(expiredBefore);

        if (expiredSeats.isEmpty()) {
            return;
        }

        log.info("[좌석 만료 스케줄러] 만료 대상 {}건 처리 시작", expiredSeats.size());

        int processed = 0;
        for (PerformanceSeat seat : expiredSeats) {
            seat.release();

            reservationRepository.findByPerformanceSeatIdAndReservationStatus(
                            seat.getPerformanceSeatId(), ReservationStatus.PENDING)
                    .ifPresent(reservation -> {
                        reservation.expire();
                        log.debug("[좌석 만료] seatId={}, reservationId={}",
                                seat.getPerformanceSeatId(), reservation.getReservationId());
                    });
            processed++;
        }

        log.info("[좌석 만료 스케줄러] {}건 처리 완료 (seat.release + reservation.expire)", processed);
    }
}
