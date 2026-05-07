package com.example.concertreservation.performance.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum PerformanceStatus {

    BOOKING, // 예매 중
    READY, // 예매 예정
    CLOSED, //예매 마감
    ;
}
