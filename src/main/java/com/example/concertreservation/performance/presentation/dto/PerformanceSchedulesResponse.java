package com.example.concertreservation.performance.presentation.dto;

import com.example.concertreservation.performance.application.result.PerformanceScheduleListResult;
import java.time.LocalDateTime;

public record PerformanceSchedulesResponse(
        Long scheduleId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer totalSeats,
        Integer availableSeats
) {

    public static PerformanceSchedulesResponse from(PerformanceScheduleListResult result) {
        return new PerformanceSchedulesResponse(
                result.scheduleId(),
                result.startTime(),
                result.endTime(),
                result.totalSeats(),
                result.availableSeats()
        );
    }
}
