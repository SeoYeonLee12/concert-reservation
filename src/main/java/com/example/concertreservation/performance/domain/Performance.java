package com.example.concertreservation.performance.domain;

import com.example.concertreservation.global.domain.SoftDeletedDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    public Performance(
            String performanceTitle,
            String performanceDescription,
            String posterImage,
            Integer runningTime,
            String ageRating
    ) {
        this.performanceTitle = performanceTitle;
        this.performanceDescription = performanceDescription;
        this.posterImage = posterImage;
        this.runningTime = runningTime;
        this.ageRating = ageRating;
    }
}
