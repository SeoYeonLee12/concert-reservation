package com.example.concertreservation.global.kafka.producer;

import com.example.concertreservation.global.event.DomainEvent;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("PAYMENT_CONFIRMED")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentConfirmedDomainEvent extends DomainEvent {

    private Long userId;
    private Integer price;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private PaymentConfirmedDomainEvent(Long reservationId, Long userId, Integer price) {
        super(reservationId);
        this.userId = userId;
        this.price = price;
    }

    public static PaymentConfirmedDomainEvent of(Long reservationId, Long userId, Integer price) {
        PaymentConfirmedDomainEvent event = new PaymentConfirmedDomainEvent(reservationId, userId, price);
        event.payload = String.format(
                "{\"uuid\":\"%s\",\"userId\":%d,\"reservationId\":%d,\"price\":%d}",
                event.getUuid(), userId, reservationId, price);
        return event;
    }

    @Override
    public String getTopic() {
        return "payment.confirmed";
    }
}
