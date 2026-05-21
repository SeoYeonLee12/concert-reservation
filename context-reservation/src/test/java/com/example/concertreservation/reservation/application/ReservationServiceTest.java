package com.example.concertreservation.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.concertreservation.global.error.errorcode.PerformanceSeatErrorCode;
import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * ReservationService 단위 테스트 — Redisson 분산 락 동작에 집중.
 * 비즈니스 로직(doReserve/doConfirmPayment/doCancelReservation)은
 * ReservationTransactionalServiceTest에서 검증.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private ReservationTransactionalService reservationTransactionalService;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void 락_획득_성공시_트랜잭션_서비스_호출하고_예약ID_반환() throws Exception {
        Long userId = 1L;
        Long seatId = 100L;
        when(redissonClient.getLock(eq("lock:seat:" + seatId))).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(reservationTransactionalService.doReserve(userId, seatId)).thenReturn(999L);

        Long reservationId = reservationService.tryReserve(userId, seatId);

        assertThat(reservationId).isEqualTo(999L);
        verify(reservationTransactionalService).doReserve(userId, seatId);
        verify(rLock).unlock();
    }

    @Test
    void 락_획득_실패시_SEAT_LOCK_TIMEOUT_예외_발생() throws Exception {
        Long seatId = 100L;
        when(redissonClient.getLock(eq("lock:seat:" + seatId))).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThatThrownBy(() -> reservationService.tryReserve(1L, seatId))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(ReservationErrorCode.SEAT_LOCK_TIMEOUT);

        verify(reservationTransactionalService, never()).doReserve(anyLong(), anyLong());
        verify(rLock, never()).unlock();
    }

    @Test
    void 락_대기중_인터럽트시_SEAT_LOCK_INTERRUPTED_예외_발생() throws Exception {
        Long seatId = 100L;
        when(redissonClient.getLock(eq("lock:seat:" + seatId))).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException());

        assertThatThrownBy(() -> reservationService.tryReserve(1L, seatId))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(ReservationErrorCode.SEAT_LOCK_INTERRUPTED);

        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void 도메인_invariant_위반시_예외_전파되고_락_반드시_해제() throws Exception {
        Long seatId = 100L;
        when(redissonClient.getLock(eq("lock:seat:" + seatId))).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(reservationTransactionalService.doReserve(anyLong(), eq(seatId)))
                .thenThrow(new GlobalException(PerformanceSeatErrorCode.NOT_RESERVABLE));

        assertThatThrownBy(() -> reservationService.tryReserve(1L, seatId))
                .isInstanceOf(GlobalException.class);

        verify(rLock).unlock();
    }
}
