package com.example.concertreservation.performanceseat.presentation.dto;

import com.example.concertreservation.performanceseat.application.result.PerformanceSeatResult;
import com.example.concertreservation.performanceseat.domain.enums.SeatStatus;

public record PerformanceSeatResponse(
        Long performanceSeatId,
        String seatNumber,
        String section,
        Integer price,
        SeatStatus seatStatus
) {

    public static PerformanceSeatResponse from(PerformanceSeatResult result) {
        return new PerformanceSeatResponse(
                result.performanceSeatId(),
                result.seatNumber(),
                result.section(),
                result.price(),
                result.seatStatus()
        );
    }
}
