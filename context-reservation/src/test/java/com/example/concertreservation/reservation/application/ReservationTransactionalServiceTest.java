package com.example.concertreservation.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.global.outbox.OutboxEvent;
import com.example.concertreservation.global.outbox.OutboxEventRepository;
import com.example.concertreservation.performanceseat.domain.PerformanceSeat;
import com.example.concertreservation.performanceseat.domain.PerformanceSeatRepository;
import com.example.concertreservation.performanceseat.domain.enums.SeatStatus;
import com.example.concertreservation.pointHistory.domain.PointHistoryRepository;
import com.example.concertreservation.reservation.domain.Reservation;
import com.example.concertreservation.reservation.domain.ReservationRepository;
import com.example.concertreservation.reservation.domain.enums.ReservationStatus;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ReservationTransactionalServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private PerformanceSeatRepository performanceSeatRepository;
    @Mock private UserRepository userRepository;
    @Mock private PointHistoryRepository pointHistoryRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservationTransactionalService reservationTransactionalService;

    // -------- doReserve --------

    @Test
    void doReserve_성공시_좌석_TEMPORARY_전이_및_예약_저장() {
        Long userId = 1L;
        Long seatId = 100L;

        when(userRepository.getUserById(userId)).thenReturn(mock(User.class));

        PerformanceSeat seat = createSeatWithStatus(SeatStatus.AVAILABLE, 50_000);
        when(performanceSeatRepository.getByPerformanceSeatId(seatId)).thenReturn(seat);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            setField(r, "reservationId", 999L);
            return r;
        });

        Long reservationId = reservationTransactionalService.doReserve(userId, seatId);

        assertThat(reservationId).isEqualTo(999L);
        assertThat(seat.getSeatStatus()).isEqualTo(SeatStatus.TEMPORARY);
        verify(reservationRepository).save(any(Reservation.class));
    }

    // -------- doConfirmPayment --------

    @Test
    void 결제_확정_성공() {
        Long userId = 1L;
        Long reservationId = 10L;

        User user = createUserWithPoint(100_000L);
        when(userRepository.findByUsersIdForUpdate(userId)).thenReturn(user);

        Reservation reservation = createReservation(userId, 100L, ReservationStatus.PENDING, 50_000);
        when(reservationRepository.getByReservationId(reservationId)).thenReturn(reservation);

        PerformanceSeat seat = createSeatWithStatus(SeatStatus.TEMPORARY, 50_000);
        when(performanceSeatRepository.getByPerformanceSeatId(100L)).thenReturn(seat);

        OutboxEvent savedEvent = mock(OutboxEvent.class);
        when(savedEvent.getId()).thenReturn(999L);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(savedEvent);

        reservationTransactionalService.doConfirmPayment(userId, reservationId);

        assertThat(seat.getSeatStatus()).isEqualTo(SeatStatus.SOLD);
        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(user.getPoint()).isEqualTo(50_000L);
        verify(pointHistoryRepository).save(any());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void 결제_확정_시_다른_사용자_예약이면_ACCESS_DENIED() {
        Long userId = 1L;
        Long otherUserId = 99L;
        Long reservationId = 10L;

        when(userRepository.findByUsersIdForUpdate(userId)).thenReturn(createUserWithPoint(100_000L));

        Reservation reservation = createReservation(otherUserId, 100L, ReservationStatus.PENDING, 50_000);
        when(reservationRepository.getByReservationId(reservationId)).thenReturn(reservation);

        assertThatThrownBy(() -> reservationTransactionalService.doConfirmPayment(userId, reservationId))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(ReservationErrorCode.RESERVATION_ACCESS_DENIED);

        verify(pointHistoryRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    // -------- doCancelReservation --------

    @Test
    void 예약_취소_환불_성공() {
        Long userId = 1L;
        Long reservationId = 10L;

        User user = createUserWithPoint(0L);
        when(userRepository.findByUsersIdForUpdate(userId)).thenReturn(user);

        Reservation reservation = createReservation(userId, 100L, ReservationStatus.CONFIRMED, 50_000);
        when(reservationRepository.getByReservationId(reservationId)).thenReturn(reservation);

        PerformanceSeat seat = createSeatWithStatus(SeatStatus.SOLD, 50_000);
        when(performanceSeatRepository.getByPerformanceSeatId(100L)).thenReturn(seat);

        reservationTransactionalService.doCancelReservation(userId, reservationId);

        assertThat(seat.getSeatStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(user.getPoint()).isEqualTo(50_000L);
        verify(pointHistoryRepository).save(any());
    }

    @Test
    void 예약_취소_시_다른_사용자_예약이면_ACCESS_DENIED() {
        Long userId = 1L;
        Long otherUserId = 99L;
        Long reservationId = 10L;

        when(userRepository.findByUsersIdForUpdate(userId)).thenReturn(createUserWithPoint(0L));

        Reservation reservation = createReservation(otherUserId, 100L, ReservationStatus.CONFIRMED, 50_000);
        when(reservationRepository.getByReservationId(reservationId)).thenReturn(reservation);

        assertThatThrownBy(() -> reservationTransactionalService.doCancelReservation(userId, reservationId))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(ReservationErrorCode.RESERVATION_ACCESS_DENIED);

        verify(pointHistoryRepository, never()).save(any());
    }

    // -------- fixtures --------

    private static User createUserWithPoint(long point) {
        try {
            var ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            User user = ctor.newInstance();
            setField(user, "usersId", 1L);
            setField(user, "point", point);
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Reservation createReservation(Long userId, Long seatId, ReservationStatus status, int price) {
        try {
            var ctor = Reservation.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Reservation reservation = ctor.newInstance();
            setField(reservation, "userId", userId);
            setField(reservation, "performanceSeatId", seatId);
            setField(reservation, "reservationStatus", status);
            setField(reservation, "price", price);
            return reservation;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PerformanceSeat createSeatWithStatus(SeatStatus status, int price) {
        try {
            var ctor = PerformanceSeat.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            PerformanceSeat seat = ctor.newInstance();
            setField(seat, "performanceSeatId", 100L);
            setField(seat, "price", price);
            setField(seat, "seatStatus", status);
            return seat;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> current = target.getClass();
            while (current != null) {
                try {
                    Field field = current.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new RuntimeException("필드를 찾을 수 없음: " + fieldName);
        } catch (Exception e) {
            throw new RuntimeException("fixture 필드 설정 실패: " + fieldName, e);
        }
    }
}
