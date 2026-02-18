package com.example.concertreservation.performance.domain;

import com.example.concertreservation.global.domain.SoftDeletedDomain;
import com.example.concertreservation.place.domain.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "reservation_start_at", nullable = false)
    private LocalDateTime reservationStartAt;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    public Schedule(
            Performance performance,
            Place place,
            LocalDateTime startTime,
            LocalDateTime reservationStartAt,
            Integer totalSeats,
            Integer availableSeats
    ) {
        this.performance = performance;
        this.place = place;
        this.startTime = startTime;
        this.reservationStartAt = reservationStartAt;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
    }
}
