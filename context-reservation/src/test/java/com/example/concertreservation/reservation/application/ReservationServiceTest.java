package com.example.concertreservation.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.reservation.application.strategy.ReservationLockStrategy;
import com.example.concertreservation.waiting.application.WaitingQueueService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * ReservationService 단위 테스트 — 전략 라우팅 동작에 집중.
 * 개별 락 전략 로직은 각 전략 테스트에서, 비즈니스 로직은 ReservationTransactionalServiceTest에서 검증.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationLockStrategy redissonStrategy;
    @Mock private ReservationLockStrategy namedLockStrategy;
    @Mock private ReservationLockStrategy optimisticStrategy;
    @Mock private ReservationLockStrategy pessimisticStrategy;
    @Mock private ReservationTransactionalService reservationTransactionalService;
    @Mock private ReservationAsyncService reservationAsyncService;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private StringRedisTemplate stringRedisTemplate;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        Map<String, ReservationLockStrategy> strategies = Map.of(
                "redisson", redissonStrategy,
                "named-lock", namedLockStrategy,
                "optimistic", optimisticStrategy,
                "pessimistic", pessimisticStrategy
        );
        reservationService = new ReservationService(
                strategies, reservationTransactionalService, reservationAsyncService,
                waitingQueueService, stringRedisTemplate);
    }

    @Test
    void redisson_전략으로_좌석_선점_성공() {
        when(redissonStrategy.tryReserve(1L, 100L)).thenReturn(999L);

        Long reservationId = reservationService.tryReserve(1L, 100L, "redisson");

        assertThat(reservationId).isEqualTo(999L);
        verify(redissonStrategy).tryReserve(1L, 100L);
        verify(namedLockStrategy, never()).tryReserve(anyLong(), anyLong());
        verify(optimisticStrategy, never()).tryReserve(anyLong(), anyLong());
        verify(waitingQueueService).markDone(1L, 100L);
    }

    @Test
    void named_lock_전략으로_좌석_선점_성공() {
        when(namedLockStrategy.tryReserve(1L, 100L)).thenReturn(888L);

        Long reservationId = reservationService.tryReserve(1L, 100L, "named-lock");

        assertThat(reservationId).isEqualTo(888L);
        verify(namedLockStrategy).tryReserve(1L, 100L);
        verify(redissonStrategy, never()).tryReserve(anyLong(), anyLong());
    }

    @Test
    void optimistic_전략으로_좌석_선점_성공() {
        when(optimisticStrategy.tryReserve(1L, 100L)).thenReturn(777L);

        Long reservationId = reservationService.tryReserve(1L, 100L, "optimistic");

        assertThat(reservationId).isEqualTo(777L);
        verify(optimisticStrategy).tryReserve(1L, 100L);
    }

    @Test
    void pessimistic_전략으로_좌석_선점_성공() {
        when(pessimisticStrategy.tryReserve(1L, 100L)).thenReturn(666L);

        Long reservationId = reservationService.tryReserve(1L, 100L, "pessimistic");

        assertThat(reservationId).isEqualTo(666L);
        verify(pessimisticStrategy).tryReserve(1L, 100L);
        verify(redissonStrategy, never()).tryReserve(anyLong(), anyLong());
    }

    @Test
    void 알_수_없는_전략은_redisson_기본값으로_폴백() {
        when(redissonStrategy.tryReserve(1L, 100L)).thenReturn(999L);

        Long reservationId = reservationService.tryReserve(1L, 100L, "unknown-strategy");

        assertThat(reservationId).isEqualTo(999L);
        verify(redissonStrategy).tryReserve(1L, 100L);
    }

    @Test
    void 좌석_선점_실패시_waitingQueue_markDone_호출_안함() {
        when(redissonStrategy.tryReserve(1L, 100L))
                .thenThrow(new GlobalException(ReservationErrorCode.SEAT_LOCK_TIMEOUT));

        assertThatThrownBy(() -> reservationService.tryReserve(1L, 100L, "redisson"))
                .isInstanceOf(GlobalException.class);

        verify(waitingQueueService, never()).markDone(anyLong(), anyLong());
    }
}
