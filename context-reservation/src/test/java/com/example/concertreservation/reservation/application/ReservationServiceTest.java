package com.example.concertreservation.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PerformanceSeatRepository performanceSeatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void 락_획득_성공시_좌석_선점_및_예약_저장() throws Exception {
        // given
        Long userId = 1L;
        Long seatId = 100L;
        when(redissonClient.getLock(eq("lock:seat:" + seatId))).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        when(userRepository.getUserById(userId)).thenReturn(mock(User.class));

        PerformanceSeat seat = createAvailableSeat(50_000);
        when(performanceSeatRepository.getByPerformanceSeatId(seatId)).thenReturn(seat);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            setField(r, "reservationId", 999L);
            return r;
        });

        // when
        Long reservationId = reservationService.tryReserve(userId, seatId);

        // then
        assertThat(reservationId).isEqualTo(999L);
        assertThat(seat.getSeatStatus()).isEqualTo(SeatStatus.TEMPORARY);
        verify(rLock).unlock();
    }

    @Test
    void 락_획득_실패시_SEAT_LOCK_TIMEOUT() throws Exception {
        // given
        Long seatId = 100L;
        when(redissonClient.getLock(eq("lock:seat:" + seatId))).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> reservationService.tryReserve(1L, seatId))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(ReservationErrorCode.SEAT_LOCK_TIMEOUT);

        verify(reservationRepository, never()).save(any());
        verify(rLock, never()).unlock();
    }

    @Test
    void 락_대기중_인터럽트시_SEAT_LOCK_INTERRUPTED() throws Exception {
        // given
        Long seatId = 100L;
        when(redissonClient.getLock(eq("lock:seat:" + seatId))).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException());

        // when / then
        assertThatThrownBy(() -> reservationService.tryReserve(1L, seatId))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(ReservationErrorCode.SEAT_LOCK_INTERRUPTED);
        // 인터럽트 플래그 복원 검증
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void 도메인_invariant_위반시_예외_전파_및_락_해제() throws Exception {
        // 이미 SOLD인 좌석을 tryReserve하면 PerformanceSeat.tryReserve가 NOT_RESERVABLE 던짐
        Long seatId = 100L;
        when(redissonClient.getLock(eq("lock:seat:" + seatId))).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        when(userRepository.getUserById(1L)).thenReturn(mock(User.class));

        PerformanceSeat seat = createSeatWithStatus(SeatStatus.SOLD);
        when(performanceSeatRepository.getByPerformanceSeatId(seatId)).thenReturn(seat);

        assertThatThrownBy(() -> reservationService.tryReserve(1L, seatId))
                .isInstanceOf(GlobalException.class);

        // 도메인 invariant 위반 시에도 락은 정상 해제되어야 함 (deadlock 방지)
        verify(rLock).unlock();
        verify(reservationRepository, never()).save(any());
    }

    // -------- confirmPayment 테스트 --------

    @Test
    void 결제_확정_성공() {
        Long userId = 1L;
        Long reservationId = 10L;

        User user = createUserWithPoint(100_000L);
        when(userRepository.findByUsersIdForUpdate(userId)).thenReturn(user);

        Reservation reservation = createReservation(userId, 100L, ReservationStatus.PENDING, 50_000);
        when(reservationRepository.getByReservationId(reservationId)).thenReturn(reservation);

        PerformanceSeat seat = createSeatWithStatus(SeatStatus.TEMPORARY);
        when(performanceSeatRepository.getByPerformanceSeatId(100L)).thenReturn(seat);

        OutboxEvent savedEvent = mock(OutboxEvent.class);
        when(savedEvent.getId()).thenReturn(999L);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(savedEvent);

        reservationService.confirmPayment(userId, reservationId);

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

        assertThatThrownBy(() -> reservationService.confirmPayment(userId, reservationId))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(ReservationErrorCode.RESERVATION_ACCESS_DENIED);

        verify(pointHistoryRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    // -------- cancelReservation 테스트 --------

    @Test
    void 예약_취소_환불_성공() {
        Long userId = 1L;
        Long reservationId = 10L;

        User user = createUserWithPoint(0L);
        when(userRepository.findByUsersIdForUpdate(userId)).thenReturn(user);

        Reservation reservation = createReservation(userId, 100L, ReservationStatus.CONFIRMED, 50_000);
        when(reservationRepository.getByReservationId(reservationId)).thenReturn(reservation);

        PerformanceSeat seat = createSeatWithStatus(SeatStatus.SOLD);
        when(performanceSeatRepository.getByPerformanceSeatId(100L)).thenReturn(seat);

        reservationService.cancelReservation(userId, reservationId);

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

        assertThatThrownBy(() -> reservationService.cancelReservation(userId, reservationId))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getCode())
                .isEqualTo(ReservationErrorCode.RESERVATION_ACCESS_DENIED);

        verify(pointHistoryRepository, never()).save(any());
    }

    // ---------------- fixtures ----------------

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

    private static PerformanceSeat createAvailableSeat(int price) {
        return createSeatWithStatus(SeatStatus.AVAILABLE, price);
    }

    private static PerformanceSeat createSeatWithStatus(SeatStatus status) {
        return createSeatWithStatus(status, 50_000);
    }

    private static PerformanceSeat createSeatWithStatus(SeatStatus status, int price) {
        PerformanceSeat seat;
        try {
            // PerformanceSeat의 @NoArgsConstructor(access = PROTECTED) 우회
            var ctor = PerformanceSeat.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            seat = ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setField(seat, "performanceSeatId", 100L);
        setField(seat, "price", price);
        setField(seat, "seatStatus", status);
        return seat;
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
