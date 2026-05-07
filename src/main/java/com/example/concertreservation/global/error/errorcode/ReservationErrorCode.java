package com.example.concertreservation.global.error.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

    NOT_PENDING(HttpStatus.CONFLICT, "RV001", "PENDING 상태의 예약이 아닙니다"),
    NOT_CONFIRMED(HttpStatus.CONFLICT, "RV002", "CONFIRMED 상태의 예약이 아닙니다"),
    NOT_CANCELLED(HttpStatus.CONFLICT, "RV003", "CANCELLED 상태의 예약이 아닙니다"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
