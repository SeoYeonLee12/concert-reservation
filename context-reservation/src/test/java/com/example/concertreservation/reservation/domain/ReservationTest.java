package com.example.concertreservation.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.reservation.domain.enums.ReservationStatus;
import org.junit.jupiter.api.Test;

class ReservationTest {

    // -------------------------------------------------------------------------
    // confirm
    // -------------------------------------------------------------------------

    @Test
    void confirm_PENDING_상태에서_호출하면_CONFIRMED로_변경된다() {
        // given
        Reservation reservation = createReservation(ReservationStatus.PENDING, 10000);

        // when
        reservation.confirm();

        // then
        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void confirm_CONFIRMED_상태에서_호출하면_NOT_PENDING_예외가_발생한다() {
        // given
        Reservation reservation = createReservation(ReservationStatus.CONFIRMED, 10000);

        // when & then
        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(ReservationErrorCode.NOT_PENDING));
    }

    @Test
    void confirm_CANCELLED_상태에서_호출하면_NOT_PENDING_예외가_발생한다() {
        // given
        Reservation reservation = createReservation(ReservationStatus.CANCELLED, 10000);

        // when & then
        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(ReservationErrorCode.NOT_PENDING));
    }

    // -------------------------------------------------------------------------
    // cancel
    // -------------------------------------------------------------------------

    @Test
    void cancel_CONFIRMED_상태에서_호출하면_CANCELLED로_변경된다() {
        // given
        Reservation reservation = createReservation(ReservationStatus.CONFIRMED, 20000);

        // when
        reservation.cancel();

        // then
        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancel_PENDING_상태에서_호출하면_NOT_CONFIRMED_예외가_발생한다() {
        // given
        Reservation reservation = createReservation(ReservationStatus.PENDING, 20000);

        // when & then
        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(ReservationErrorCode.NOT_CONFIRMED));
    }

    // -------------------------------------------------------------------------
    // refundAmount
    // -------------------------------------------------------------------------

    @Test
    void refundAmount_CANCELLED_상태에서_price를_Long으로_반환한다() {
        // given
        Reservation reservation = createReservation(ReservationStatus.CANCELLED, 35000);

        // when
        Long refund = reservation.refundAmount();

        // then
        assertThat(refund).isEqualTo(35000L);
    }

    @Test
    void refundAmount_PENDING_상태에서_호출하면_NOT_CANCELLED_예외가_발생한다() {
        // given
        Reservation reservation = createReservation(ReservationStatus.PENDING, 35000);

        // when & then
        assertThatThrownBy(reservation::refundAmount)
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(ReservationErrorCode.NOT_CANCELLED));
    }

    @Test
    void refundAmount_CONFIRMED_상태에서_호출하면_NOT_CANCELLED_예외가_발생한다() {
        // given
        Reservation reservation = createReservation(ReservationStatus.CONFIRMED, 35000);

        // when & then
        assertThatThrownBy(reservation::refundAmount)
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(ReservationErrorCode.NOT_CANCELLED));
    }

    // -------------------------------------------------------------------------
    // Fixture helpers
    // -------------------------------------------------------------------------

    /**
     * user/performanceSeat는 도메인 메서드에서 참조하지 않으므로 null로 주입한다.
     * reservationId는 @GeneratedValue 필드라 리플렉션으로 주입한다.
     */
    private static Reservation createReservation(ReservationStatus status, Integer price) {
        Reservation reservation = new Reservation(null, null, status, price);
        setField(reservation, "reservationId", 1L);
        return reservation;
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
