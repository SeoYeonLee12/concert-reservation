package com.example.concertreservation.performance.application.result;

import com.example.concertreservation.performance.domain.Performance;
import com.example.concertreservation.performance.domain.Schedule;
import java.time.LocalDateTime;
import java.util.List;

public record PerformanceListResult(
        Long performanceId,
        String performanceTitle,
        String posterImage,
        String placeName,
        LocalDateTime scheduleStartTime,
        LocalDateTime scheduleEndTime,
        String performanceStatus
) {

    public static PerformanceListResult from(Performance performance) {
        List<Schedule> schedules = performance.getSchedules();

        Schedule firstSchedule = schedules.getFirst();
        Schedule lastSchedule = schedules.getLast();

        return new PerformanceListResult(
                performance.getPerformanceId(),
                performance.getPerformanceTitle(),
                performance.getPosterImage(),
                firstSchedule.getPlace().getPlaceName(),
                firstSchedule.getStartTime(),
                lastSchedule.getEndTime(),
                performance.getPerformanceStatus().name()
        );
    }
}
