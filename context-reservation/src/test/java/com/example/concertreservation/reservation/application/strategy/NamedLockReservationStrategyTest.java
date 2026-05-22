package com.example.concertreservation.reservation.application.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.reservation.application.ReservationTransactionalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NamedLockReservationStrategyTest {

    @Mock private NamedLockService namedLockService;
    @Mock private ReservationTransactionalService reservationTransactionalService;

    @InjectMocks private NamedLockReservationStrategy strategy;

    @Test
    void 락_획득_성공시_예약_처리_후_락_해제() {
        Long userId = 1L;
        Long seatId = 100L;
        when(reservationTransactionalService.doReserve(userId, seatId)).thenReturn(999L);

        Long reservationId = strategy.tryReserve(userId, seatId);

        assertThat(reservationId).isEqualTo(999L);
        verify(namedLockService).getLock("seat:100");
        verify(reservationTransactionalService).doReserve(userId, seatId);
        verify(namedLockService).releaseLock("seat:100");
    }

    @Test
    void 락_획득_실패시_SEAT_LOCK_TIMEOUT_예외() {
        Long seatId = 100L;
        doThrow(new GlobalException(ReservationErrorCode.SEAT_LOCK_TIMEOUT))
                .when(namedLockService).getLock(anyString());

        assertThatThrownBy(() -> strategy.tryReserve(1L, seatId))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(ReservationErrorCode.SEAT_LOCK_TIMEOUT);

        verify(reservationTransactionalService, never()).doReserve(anyLong(), anyLong());
        verify(namedLockService).releaseLock("seat:100");
    }

    @Test
    void 비즈니스_로직_예외시에도_락_반드시_해제() {
        Long seatId = 100L;
        when(reservationTransactionalService.doReserve(1L, seatId))
                .thenThrow(new GlobalException(ReservationErrorCode.SEAT_LOCK_TIMEOUT));

        assertThatThrownBy(() -> strategy.tryReserve(1L, seatId))
                .isInstanceOf(GlobalException.class);

        verify(namedLockService).releaseLock("seat:100");
    }
}
