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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class OptimisticLockReservationStrategyTest {

    @Mock private ReservationTransactionalService reservationTransactionalService;

    @InjectMocks private OptimisticLockReservationStrategy strategy;

    @Test
    void 충돌_없으면_1회_호출로_성공() {
        when(reservationTransactionalService.doReserve(1L, 100L)).thenReturn(999L);

        Long reservationId = strategy.tryReserve(1L, 100L);

        assertThat(reservationId).isEqualTo(999L);
        verify(reservationTransactionalService, times(1)).doReserve(1L, 100L);
    }

    @Test
    void NOT_RESERVABLE_예외는_재시도_없이_즉시_전파() {
        // 낙관적 락 예외가 아닌 비즈니스 예외 → RetryAspect가 잡지 않음
        // 단위 테스트에서는 AOP가 적용되지 않으므로 직접 예외 전파 검증
        when(reservationTransactionalService.doReserve(1L, 100L))
                .thenThrow(new GlobalException(PerformanceSeatErrorCode.NOT_RESERVABLE));

        assertThatThrownBy(() -> strategy.tryReserve(1L, 100L))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(PerformanceSeatErrorCode.NOT_RESERVABLE);

        verify(reservationTransactionalService, times(1)).doReserve(1L, 100L);
    }

    @Test
    void ObjectOptimisticLockingFailureException은_전략에서_위로_전파() {
        // AOP 없는 단위 테스트: 예외가 위로 전파되는지 확인
        // 실제 RetryAspect 동작은 Spring 컨텍스트 통합 테스트에서 검증
        when(reservationTransactionalService.doReserve(1L, 100L))
                .thenThrow(new ObjectOptimisticLockingFailureException(Object.class, 1L));

        assertThatThrownBy(() -> strategy.tryReserve(1L, 100L))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
