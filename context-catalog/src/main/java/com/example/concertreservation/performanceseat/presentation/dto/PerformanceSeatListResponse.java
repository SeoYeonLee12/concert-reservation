package com.example.concertreservation.performanceseat.presentation.dto;

import com.example.concertreservation.performanceseat.application.result.PerformanceSeatResult;
import java.util.List;

public record PerformanceSeatListResponse(
        List<PerformanceSeatResponse> seats
) {

    public static PerformanceSeatListResponse from(List<PerformanceSeatResult> results) {
        List<PerformanceSeatResponse> seats = results.stream()
                .map(PerformanceSeatResponse::from)
                .toList();
        return new PerformanceSeatListResponse(seats);
    }
}
