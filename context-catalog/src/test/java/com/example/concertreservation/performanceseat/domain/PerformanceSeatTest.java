package com.example.concertreservation.performanceseat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.concertreservation.global.error.errorcode.PerformanceSeatErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.performanceseat.domain.enums.SeatStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PerformanceSeatTest {

    // -------------------------------------------------------------------------
    // tryReserve
    // -------------------------------------------------------------------------

    @Test
    void tryReserve_AVAILABLE_상태에서_호출하면_TEMPORARY로_전이되고_reservedAt이_갱신된다() {
        PerformanceSeat seat = createSeat(SeatStatus.AVAILABLE, LocalDateTime.of(2026, 1, 1, 0, 0));
        LocalDateTime now = LocalDateTime.of(2026, 5, 7, 10, 0);

        seat.tryReserve(now);

        assertThat(seat.getSeatStatus()).isEqualTo(SeatStatus.TEMPORARY);
        assertThat(seat.getReservedAt()).isEqualTo(now);
    }

    @Test
    void tryReserve_TEMPORARY_상태에서_호출하면_NOT_RESERVABLE_예외가_발생한다() {
        PerformanceSeat seat = createSeat(SeatStatus.TEMPORARY, LocalDateTime.now());

        assertThatThrownBy(() -> seat.tryReserve(LocalDateTime.now()))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(PerformanceSeatErrorCode.NOT_RESERVABLE));
    }

    @Test
    void tryReserve_SOLD_상태에서_호출하면_NOT_RESERVABLE_예외가_발생한다() {
        PerformanceSeat seat = createSeat(SeatStatus.SOLD, LocalDateTime.now());

        assertThatThrownBy(() -> seat.tryReserve(LocalDateTime.now()))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(PerformanceSeatErrorCode.NOT_RESERVABLE));
    }

    // -------------------------------------------------------------------------
    // confirmReservation
    // -------------------------------------------------------------------------

    @Test
    void confirmReservation_TEMPORARY_상태에서_호출하면_SOLD로_전이된다() {
        PerformanceSeat seat = createSeat(SeatStatus.TEMPORARY, LocalDateTime.now());

        seat.confirmReservation();

        assertThat(seat.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
    }

    @Test
    void confirmReservation_AVAILABLE_상태에서_호출하면_NOT_TEMPORARY_예외가_발생한다() {
        PerformanceSeat seat = createSeat(SeatStatus.AVAILABLE, LocalDateTime.now());

        assertThatThrownBy(() -> seat.confirmReservation())
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(PerformanceSeatErrorCode.NOT_TEMPORARY));
    }

    // -------------------------------------------------------------------------
    // release
    // -------------------------------------------------------------------------

    @Test
    void release_TEMPORARY_상태에서_호출하면_AVAILABLE로_전이되고_reservedAt은_유지된다() {
        LocalDateTime reservedAt = LocalDateTime.of(2026, 5, 7, 9, 0);
        PerformanceSeat seat = createSeat(SeatStatus.TEMPORARY, reservedAt);

        seat.release();

        assertThat(seat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seat.getReservedAt()).isEqualTo(reservedAt);
    }

    @Test
    void release_SOLD_상태에서_호출하면_NOT_TEMPORARY_예외가_발생한다() {
        PerformanceSeat seat = createSeat(SeatStatus.SOLD, LocalDateTime.now());

        assertThatThrownBy(() -> seat.release())
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(PerformanceSeatErrorCode.NOT_TEMPORARY));
    }

    // -------------------------------------------------------------------------
    // isExpired
    // -------------------------------------------------------------------------

    @Test
    void isExpired_TEMPORARY이고_reservedAt_더하기_window가_now보다_이전이면_true를_반환한다() {
        LocalDateTime reservedAt = LocalDateTime.of(2026, 5, 7, 9, 0);
        PerformanceSeat seat = createSeat(SeatStatus.TEMPORARY, reservedAt);
        Duration window = Duration.ofMinutes(10);
        LocalDateTime now = reservedAt.plusMinutes(11);

        assertThat(seat.isExpired(now, window)).isTrue();
    }

    @Test
    void isExpired_TEMPORARY이고_reservedAt_더하기_window가_now_이후이면_false를_반환한다() {
        LocalDateTime reservedAt = LocalDateTime.of(2026, 5, 7, 9, 0);
        PerformanceSeat seat = createSeat(SeatStatus.TEMPORARY, reservedAt);
        Duration window = Duration.ofMinutes(10);
        LocalDateTime now = reservedAt.plusMinutes(9);

        assertThat(seat.isExpired(now, window)).isFalse();
    }

    @Test
    void isExpired_AVAILABLE_또는_SOLD_상태이면_항상_false를_반환한다() {
        LocalDateTime reservedAt = LocalDateTime.of(2026, 5, 7, 9, 0);
        Duration window = Duration.ofMinutes(1);
        LocalDateTime now = reservedAt.plusHours(1);

        PerformanceSeat available = createSeat(SeatStatus.AVAILABLE, reservedAt);
        PerformanceSeat sold = createSeat(SeatStatus.SOLD, reservedAt);

        assertThat(available.isExpired(now, window)).isFalse();
        assertThat(sold.isExpired(now, window)).isFalse();
    }

    // -------------------------------------------------------------------------
    // isReservable
    // -------------------------------------------------------------------------

    @Test
    void isReservable_AVAILABLE이면_true_그_외_상태는_false를_반환한다() {
        PerformanceSeat available = createSeat(SeatStatus.AVAILABLE, LocalDateTime.now());
        PerformanceSeat temporary = createSeat(SeatStatus.TEMPORARY, LocalDateTime.now());
        PerformanceSeat sold = createSeat(SeatStatus.SOLD, LocalDateTime.now());

        assertThat(available.isReservable()).isTrue();
        assertThat(temporary.isReservable()).isFalse();
        assertThat(sold.isReservable()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Fixture helpers
    // -------------------------------------------------------------------------

    private static PerformanceSeat createSeat(SeatStatus status, LocalDateTime reservedAt) {
        PerformanceSeat seat = new PerformanceSeat();
        setField(seat, "seatStatus", status);
        setField(seat, "reservedAt", reservedAt);
        return seat;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("fixture 필드 설정 실패: " + fieldName, e);
        }
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없음: " + name);
    }
}
