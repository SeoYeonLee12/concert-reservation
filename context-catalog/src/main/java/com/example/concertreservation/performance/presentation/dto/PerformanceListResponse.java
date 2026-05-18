package com.example.concertreservation.performance.presentation.dto;

import com.example.concertreservation.performance.application.result.PerformanceListResult;
import java.util.List;

public record PerformanceListResponse(
        List<PerformancesResponse> performances
) {

    public static PerformanceListResponse from(List<PerformanceListResult> results) {
        List<PerformancesResponse> performanceResponses = results.stream()
                .map(PerformancesResponse::from)
                .toList();

        return new PerformanceListResponse(performanceResponses);
    }
}
