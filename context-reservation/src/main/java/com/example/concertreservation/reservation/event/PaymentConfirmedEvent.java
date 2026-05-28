package com.example.concertreservation.reservation.event;

public record PaymentConfirmedEvent(Long domainEventId, String uuid, Long userId, Long reservationId, Integer price) {}
