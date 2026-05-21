package com.example.concertreservation.waiting.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record WaitingQueueEnterRequest(
        @NotNull Long performanceSeatId
) {}
