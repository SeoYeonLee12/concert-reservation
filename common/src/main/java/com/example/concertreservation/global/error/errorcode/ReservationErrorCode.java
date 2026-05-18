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
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RV004", "예약을 찾을 수 없습니다"),
    SEAT_LOCK_TIMEOUT(HttpStatus.CONFLICT, "RV005", "좌석 선점 락 획득 실패 (다른 사용자가 처리 중)"),
    SEAT_LOCK_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "RV006", "좌석 선점 락 대기 중 인터럽트"),
    RESERVATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RV007", "해당 예약에 접근할 권한이 없습니다"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
