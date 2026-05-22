package com.example.concertreservation.waiting.presentation.dto;

public record WaitingQueueStatusResponse(
        Long waitingQueueId,
        String status,
        Integer queuePosition,
        Integer aheadCount,
        Boolean isActive
) {}
