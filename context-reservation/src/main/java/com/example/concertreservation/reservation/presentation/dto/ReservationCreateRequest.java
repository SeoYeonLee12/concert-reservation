package com.example.concertreservation.reservation.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record ReservationCreateRequest(
        @NotNull Long performanceSeatId
) {
}
