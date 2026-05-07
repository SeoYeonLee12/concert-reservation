package com.example.concertreservation.performance.application.result;

import com.example.concertreservation.performance.domain.Schedule;
import java.time.LocalDateTime;

public record PerformanceScheduleListResult(
        Long scheduleId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer totalSeats,
        Integer availableSeats
) {

    public static PerformanceScheduleListResult from(Schedule schedule) {
        return new PerformanceScheduleListResult(
                schedule.getScheduleId(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getTotalSeats(),
                schedule.getAvailableSeats()
        );
    }
}
