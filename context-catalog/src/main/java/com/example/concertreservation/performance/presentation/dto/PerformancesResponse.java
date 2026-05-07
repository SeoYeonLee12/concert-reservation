package com.example.concertreservation.performance.presentation.dto;

import com.example.concertreservation.performance.application.result.PerformanceListResult;
import java.time.LocalDateTime;

public record PerformancesResponse(
        Long performanceId,
        String performanceTitle,
        String posterImage,
        String placeName,
        LocalDateTime scheduleStartTime,
        LocalDateTime scheduleEndTime,
        String performanceStatus
) {

    public static PerformancesResponse from(PerformanceListResult result) {
        return new PerformancesResponse(
                result.performanceId(),
                result.performanceTitle(),
                result.posterImage(),
                result.placeName(),
                result.scheduleStartTime(),
                result.scheduleEndTime(),
                result.performanceStatus()
        );
    }

}
