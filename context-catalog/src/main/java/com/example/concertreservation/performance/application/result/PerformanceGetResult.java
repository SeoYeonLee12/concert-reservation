package com.example.concertreservation.performance.application.result;

import com.example.concertreservation.performance.domain.Performance;
import com.example.concertreservation.performance.domain.Schedule;
import java.time.LocalDateTime;
import java.util.List;

public record PerformanceGetResult(
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

    public static PerformanceGetResult from(Performance performance) {
        List<Schedule> schedules = performance.getSchedules();

        Schedule firstSchedule = schedules.getFirst();
        Schedule lastSchedule = schedules.getLast();

        return new PerformanceGetResult(
                performance.getPerformanceId(),
                performance.getPerformanceTitle(),
                performance.getPerformanceDescription(),
                performance.getPosterImage(),
                firstSchedule.getPlace().getPlaceName(),
                firstSchedule.getStartTime(),
                lastSchedule.getEndTime(),
                performance.getPerformanceStatus().toString(),
                performance.getPerformer(),
                performance.getAgeRating()
        );
    }
}