package com.example.concertreservation.reservation.domain;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationId(Long reservationId);

    default Reservation getByReservationId(Long reservationId) {
        return findByReservationId(reservationId).orElseThrow(
                () -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    // MY-01 마이 예매 내역 조회 — Long userId 참조 (cross-context entity 직접 결합 회피)
    Page<Reservation> findAllByUserId(Long userId, Pageable pageable);
}
