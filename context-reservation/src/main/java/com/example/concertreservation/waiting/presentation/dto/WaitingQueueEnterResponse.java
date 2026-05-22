package com.example.concertreservation.waiting.presentation.dto;

public record WaitingQueueEnterResponse(
        Long waitingQueueId,
        Integer queuePosition,
        String status
) {}
