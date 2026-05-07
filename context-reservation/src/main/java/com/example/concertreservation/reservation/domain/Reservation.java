package com.example.concertreservation.reservation.domain;

import com.example.concertreservation.global.domain.SoftDeletedDomain;
import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.performanceseat.domain.PerformanceSeat;
import com.example.concertreservation.reservation.domain.enums.ReservationStatus;
import com.example.concertreservation.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_seat_id", nullable = false)
    private PerformanceSeat performanceSeat;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    private ReservationStatus reservationStatus;

    @Column(nullable = false)
    private Integer price;

    public Reservation(
            User user,
            PerformanceSeat performanceSeat,
            ReservationStatus reservationStatus,
            Integer price
    ) {
        this.user = user;
        this.performanceSeat = performanceSeat;
        this.reservationStatus = reservationStatus;
        this.price = price;
    }

    public void confirm() {
        if (this.reservationStatus != ReservationStatus.PENDING) {
            throw new GlobalException(ReservationErrorCode.NOT_PENDING);
        }
        this.reservationStatus = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.reservationStatus != ReservationStatus.CONFIRMED) {
            throw new GlobalException(ReservationErrorCode.NOT_CONFIRMED);
        }
        this.reservationStatus = ReservationStatus.CANCELLED;
    }

    public Long refundAmount() {
        if (this.reservationStatus != ReservationStatus.CANCELLED) {
            throw new GlobalException(ReservationErrorCode.NOT_CANCELLED);
        }
        return (long) price;
    }
}
