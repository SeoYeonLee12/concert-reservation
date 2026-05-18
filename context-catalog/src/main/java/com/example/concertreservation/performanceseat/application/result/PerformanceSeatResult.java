package com.example.concertreservation.performanceseat.application.result;

import com.example.concertreservation.performanceseat.domain.PerformanceSeat;
import com.example.concertreservation.performanceseat.domain.enums.SeatStatus;

public record PerformanceSeatResult(
        Long performanceSeatId,
        String seatNumber,
        String section,
        Integer price,
        SeatStatus seatStatus
) {

    public static PerformanceSeatResult from(PerformanceSeat performanceSeat) {
        return new PerformanceSeatResult(
                performanceSeat.getPerformanceSeatId(),
                performanceSeat.getSeat().getSeatNumber(),
                performanceSeat.getSeat().getSection(),
                performanceSeat.getPrice(),
                performanceSeat.getSeatStatus()
        );
    }
}
