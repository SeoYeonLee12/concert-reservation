package com.example.concertreservation.reservation.event;

public record PaymentConfirmedEvent(Long outboxEventId, Long userId, Long reservationId, Integer price) {}
