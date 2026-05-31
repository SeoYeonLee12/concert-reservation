package com.example.concertreservation.global.kafka.producer;

public record PaymentConfirmedEvent(Long domainEventId, String uuid, Long userId, Long reservationId, Integer price) {}
