package com.example.concertreservation.performanceseat.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatStatus {

    AVAILABLE("예약 가능"),
    TEMPORARY("임시 배정"),
    SOLD("판매 완료"),
    ;

    private final String description;
}
