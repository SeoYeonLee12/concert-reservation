package com.example.concertreservation.performanceseat.domain;

import com.example.concertreservation.global.domain.SoftDeletedDomain;
import com.example.concertreservation.global.error.errorcode.PerformanceSeatErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.performance.domain.Schedule;
import com.example.concertreservation.performanceseat.domain.enums.SeatStatus;
import com.example.concertreservation.place.domain.Seat;
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
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "performance_seat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceSeat extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "performance_seat_id")
    private Long performanceSeatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    private SeatStatus seatStatus;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Version
    private Integer version;

    public void tryReserve(LocalDateTime now) {
        if (seatStatus != SeatStatus.AVAILABLE) {
            throw new GlobalException(PerformanceSeatErrorCode.NOT_RESERVABLE);
        }
        seatStatus = SeatStatus.TEMPORARY;
        reservedAt = now;
    }

    public void confirmReservation() {
        if (seatStatus != SeatStatus.TEMPORARY) {
            throw new GlobalException(PerformanceSeatErrorCode.NOT_TEMPORARY);
        }
        seatStatus = SeatStatus.SOLD;
    }

    public void release() {
        if (seatStatus != SeatStatus.TEMPORARY) {
            throw new GlobalException(PerformanceSeatErrorCode.NOT_TEMPORARY);
        }
        seatStatus = SeatStatus.AVAILABLE;
    }

    public boolean isExpired(LocalDateTime now, Duration window) {
        if (seatStatus != SeatStatus.TEMPORARY) {
            return false;
        }
        return reservedAt.plus(window).isBefore(now);
    }

    public boolean isReservable() {
        return seatStatus == SeatStatus.AVAILABLE;
    }
}
