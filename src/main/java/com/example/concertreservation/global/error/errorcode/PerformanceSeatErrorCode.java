package com.example.concertreservation.global.error.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PerformanceSeatErrorCode implements ErrorCode {

    NOT_RESERVABLE(HttpStatus.CONFLICT, "PS001", "예약 가능한 좌석이 아닙니다"),
    NOT_TEMPORARY(HttpStatus.CONFLICT, "PS002", "임시 배정 상태의 좌석이 아닙니다"),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "PS003", "좌석을 찾을 수 없습니다"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
