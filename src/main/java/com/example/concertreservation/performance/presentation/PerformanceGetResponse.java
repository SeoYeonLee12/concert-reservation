package com.example.concertreservation.performance.presentation;

import com.example.concertreservation.performance.application.result.PerformanceGetResult;
import java.time.LocalDateTime;

public record PerformanceGetResponse(
        Long performanceId,
        String performanceTitle,
        String performanceDescription,
        String posterImage,
        String placeName,
        LocalDateTime scheduleStartTime,
        LocalDateTime scheduleEndTime,
        String performanceStatus,
        String performer,
        String ageRating
) {

    public static PerformanceGetResponse from(PerformanceGetResult result) {
        return new PerformanceGetResponse(
                result.performanceId(),
                result.performanceTitle(),
                result.performanceDescription(),
                result.posterImage(),
                result.placeName(),
                result.scheduleStartTime(),
                result.scheduleEndTime(),
                result.performanceStatus(),
                result.performer(),
                result.ageRating()
        );
    }
}
