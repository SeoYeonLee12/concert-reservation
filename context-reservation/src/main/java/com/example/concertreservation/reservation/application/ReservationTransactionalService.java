package com.example.concertreservation.reservation.application;

import com.example.concertreservation.global.event.DomainEvent;
import com.example.concertreservation.global.event.DomainEventRepository;
import com.example.concertreservation.performanceseat.domain.PerformanceSeat;
import com.example.concertreservation.performanceseat.domain.PerformanceSeatRepository;
import com.example.concertreservation.pointHistory.domain.PointHistory;
import com.example.concertreservation.pointHistory.domain.PointHistoryRepository;
import com.example.concertreservation.reservation.domain.Reservation;
import com.example.concertreservation.reservation.domain.ReservationRepository;
import com.example.concertreservation.reservation.domain.enums.ReservationStatus;
import com.example.concertreservation.reservation.event.PaymentConfirmedDomainEvent;
import com.example.concertreservation.reservation.event.PaymentConfirmedEvent;
import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좌석 선점 트랜잭션 경계.
 * ReservationService에서 Redisson 락을 잡은 상태로 호출하므로,
 * 이 메서드가 반환(= 커밋 완료)된 뒤에 락이 해제된다.
 */
@Service
@RequiredArgsConstructor
public class ReservationTransactionalService {

    private final ReservationRepository reservationRepository;
    private final PerformanceSeatRepository performanceSeatRepository;
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final DomainEventRepository domainEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long doReserve(Long userId, Long performanceSeatId) {
        userRepository.getUserById(userId);
        PerformanceSeat seat = performanceSeatRepository.getByPerformanceSeatId(performanceSeatId);

        seat.tryReserve(LocalDateTime.now());

        Reservation reservation = new Reservation(
                userId, performanceSeatId, ReservationStatus.PENDING, seat.getPrice());
        reservationRepository.save(reservation);

        return reservation.getReservationId();
    }

    // 비관적 락 전용. SELECT FOR UPDATE로 락 획득 → 트랜잭션 커밋 시 자동 해제.
    @Transactional
    public Long doReserveWithPessimisticLock(Long userId, Long performanceSeatId) {
        userRepository.getUserById(userId);
        PerformanceSeat seat = performanceSeatRepository.getByPerformanceSeatIdWithPessimisticLock(performanceSeatId);
        seat.tryReserve(LocalDateTime.now());
        Reservation reservation = new Reservation(
                userId, performanceSeatId, ReservationStatus.PENDING, seat.getPrice());
        reservationRepository.save(reservation);
        return reservation.getReservationId();
    }

    /**
     * 결제 확정: 비즈니스 로직 + DomainEvent 저장 + Spring 이벤트 발행을 단일 트랜잭션으로 처리.
     * 커밋 성공 후 PaymentEventListener가 비동기로 Kafka에 발행.
     */
    @Transactional
    public void doConfirmPayment(Long userId, Long reservationId) {
        User user = userRepository.findByUsersIdForUpdate(userId);
        Reservation reservation = reservationRepository.getByReservationId(reservationId);

        if (!reservation.getUserId().equals(userId)) {
            throw new GlobalException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }

        PerformanceSeat seat = performanceSeatRepository.getByPerformanceSeatId(reservation.getPerformanceSeatId());

        user.deductPoint((long) reservation.getPrice());
        reservation.confirm();
        seat.confirmReservation();

        pointHistoryRepository.save(
                PointHistory.useHistory(user, reservationId, (long) reservation.getPrice(), user.getPoint()));

        DomainEvent domainEvent = domainEventRepository.save(
                PaymentConfirmedDomainEvent.of(reservationId, userId, reservation.getPrice()));

        eventPublisher.publishEvent(
                new PaymentConfirmedEvent(domainEvent.getId(), domainEvent.getUuid(), userId, reservationId, reservation.getPrice()));
    }

    @Transactional
    public void doCancelReservation(Long userId, Long reservationId) {
        User user = userRepository.findByUsersIdForUpdate(userId);
        Reservation reservation = reservationRepository.getByReservationId(reservationId);

        if (!reservation.getUserId().equals(userId)) {
            throw new GlobalException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }

        PerformanceSeat seat = performanceSeatRepository.getByPerformanceSeatId(reservation.getPerformanceSeatId());

        reservation.cancel();
        long refundAmount = reservation.refundAmount();
        seat.cancelReservation();
        user.chargedPoint(refundAmount);

        pointHistoryRepository.save(
                PointHistory.refundHistory(user, reservationId, refundAmount, user.getPoint()));
    }
}
