package com.example.concertreservation.pointHistory.domain;

import com.example.concertreservation.global.domain.SoftDeletedDomain;
import com.example.concertreservation.pointHistory.domain.enums.CollectType;
import com.example.concertreservation.reservation.domain.Reservation;
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
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "point_history")
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_history_id")
    private Long pointHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type")
    private CollectType collectType;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "current_point", nullable = false)
    private Long currentPoint;

    private PointHistory(
            User user,
            Reservation reservation,
            CollectType collectType,
            Long amount,
            Long currentPoint
    ) {
        this.user = user;
        this.reservation = reservation;
        this.collectType = collectType;
        this.amount = amount;
        this.currentPoint = currentPoint;
    }

    public PointHistory chargeHistory(User user, Long amount, Long currentPoint) {
        return new PointHistory(user, null, CollectType.CHARGE, amount, currentPoint);
    }

    public PointHistory useHistory(
            User user,
            Reservation reservation,
            Long amount,
            Long currentPoint
    ) {
        return new PointHistory(user, reservation, CollectType.USE, amount, currentPoint);
    }

    public PointHistory refundHistory(
            User user,
            Reservation reservation,
            Long amount,
            Long currentPoint
    ) {
        return new PointHistory(user, reservation, CollectType.REFUND, amount, currentPoint);
    }
}
