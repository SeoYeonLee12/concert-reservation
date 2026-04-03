package com.example.concertreservation.performance.domain;

import com.example.concertreservation.global.domain.SoftDeletedDomain;
import com.example.concertreservation.performance.domain.enums.PerformanceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "performance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "performance_id")
    private Long performanceId;

    @Column(name = "title", nullable = false)
    private String performanceTitle;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String performanceDescription;

    @Column(name = "poster_image", nullable = false)
    private String posterImage;

    @Column(name = "running_time", nullable = false)
    private Integer runningTime;

    @Column(name = "age_rating", nullable = false)
    private String ageRating;

    @OrderBy("startTime ASC")
    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_status")
    private PerformanceStatus performanceStatus;

    @Column(name = "performer", nullable = false)
    private String performer;

    public Performance(
            String performanceTitle,
            String performanceDescription,
            String posterImage,
            Integer runningTime,
            String ageRating,
            String performer

    ) {
        this.performanceTitle = performanceTitle;
        this.performanceDescription = performanceDescription;
        this.posterImage = posterImage;
        this.runningTime = runningTime;
        this.ageRating = ageRating;
        this.schedules = new ArrayList<>();
        this.performanceStatus = PerformanceStatus.READY;
        this.performer = performer;
    }

    public void addSchedule(Schedule schedule) {
        this.schedules.add(schedule);
        schedule.addPerformance(this);
    }
}
