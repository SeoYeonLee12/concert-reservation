package com.example.concertreservation.reservation.application.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.concertreservation.global.error.errorcode.PerformanceSeatErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.reservation.application.ReservationTransactionalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

@ExtendWith(MockitoExtension.class)
class PessimisticLockReservationStrategyTest {

    @Mock private ReservationTransactionalService reservationTransactionalService;

    @InjectMocks private PessimisticLockReservationStrategy strategy;

    @Test
    void 예약_성공시_reservationId_반환() {
        when(reservationTransactionalService.doReserveWithPessimisticLock(1L, 100L)).thenReturn(999L);

        Long reservationId = strategy.tryReserve(1L, 100L);

        assertThat(reservationId).isEqualTo(999L);
        verify(reservationTransactionalService, times(1)).doReserveWithPessimisticLock(1L, 100L);
    }

    @Test
    void NOT_RESERVABLE_예외는_즉시_전파_재시도_없음() {
        // 비관적 락은 @Retry AOP 없음 — 비즈니스 예외 즉시 상위 전파
        when(reservationTransactionalService.doReserveWithPessimisticLock(1L, 100L))
                .thenThrow(new GlobalException(PerformanceSeatErrorCode.NOT_RESERVABLE));

        assertThatThrownBy(() -> strategy.tryReserve(1L, 100L))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(PerformanceSeatErrorCode.NOT_RESERVABLE);

        verify(reservationTransactionalService, times(1)).doReserveWithPessimisticLock(1L, 100L);
    }

    @Test
    void DB_락_대기_타임아웃_예외_전파() {
        // MySQL error 1205: innodb_lock_wait_timeout 초과 시 Spring이 CannotAcquireLockException으로 변환
        // GlobalExceptionHandler가 CannotAcquireLockException을 명시적으로 처리 → 409 SEAT_CONFLICT 반환
        when(reservationTransactionalService.doReserveWithPessimisticLock(1L, 100L))
                .thenThrow(new CannotAcquireLockException("lock wait timeout exceeded"));

        assertThatThrownBy(() -> strategy.tryReserve(1L, 100L))
                .isInstanceOf(CannotAcquireLockException.class);

        verify(reservationTransactionalService, times(1)).doReserveWithPessimisticLock(1L, 100L);
    }
}
