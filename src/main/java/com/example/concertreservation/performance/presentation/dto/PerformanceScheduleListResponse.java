package com.example.concertreservation.performance.presentation.dto;

import com.example.concertreservation.performance.application.result.PerformanceScheduleListResult;
import java.util.List;

public record PerformanceScheduleListResponse(
        List<PerformanceSchedulesResponse> schedules
) {

    public static PerformanceScheduleListResponse from(List<PerformanceScheduleListResult> result) {
        List<PerformanceSchedulesResponse> schedules = result.stream()
                .map(PerformanceSchedulesResponse::from)
                .toList();

        return new PerformanceScheduleListResponse(schedules);
    }
}
