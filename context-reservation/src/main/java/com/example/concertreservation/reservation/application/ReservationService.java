package com.example.concertreservation.reservation.application;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.performanceseat.domain.PerformanceSeat;
import com.example.concertreservation.performanceseat.domain.PerformanceSeatRepository;
import com.example.concertreservation.reservation.domain.Reservation;
import com.example.concertreservation.reservation.domain.ReservationRepository;
import com.example.concertreservation.reservation.domain.enums.ReservationStatus;
import com.example.concertreservation.user.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final String SEAT_LOCK_KEY_PREFIX = "lock:seat:";
    private static final long LOCK_WAIT_TIME_MS = 3_000L;
    private static final long LOCK_LEASE_TIME_MS = 5_000L;

    private final ReservationRepository reservationRepository;
    private final PerformanceSeatRepository performanceSeatRepository;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;

    /**
     * RES-01 좌석 선점.
     * Redisson 분산 락으로 동일 좌석에 대한 동시 선점을 직렬화한 뒤,
     * 도메인 메서드 PerformanceSeat.tryReserve가 상태 invariant를 검증한다.
     */
    @Transactional
    public Long tryReserve(Long userId, Long performanceSeatId) {
        RLock lock = redissonClient.getLock(SEAT_LOCK_KEY_PREFIX + performanceSeatId);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_MS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new GlobalException(ReservationErrorCode.SEAT_LOCK_TIMEOUT);
            }

            // 사용자 존재 검증만 수행 (cross-context entity는 보유하지 않음)
            userRepository.getUserById(userId);
            PerformanceSeat seat = performanceSeatRepository.getByPerformanceSeatId(performanceSeatId);

            seat.tryReserve(LocalDateTime.now());

            Reservation reservation = new Reservation(
                    userId, performanceSeatId, ReservationStatus.PENDING, seat.getPrice());
            reservationRepository.save(reservation);

            return reservation.getReservationId();
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
