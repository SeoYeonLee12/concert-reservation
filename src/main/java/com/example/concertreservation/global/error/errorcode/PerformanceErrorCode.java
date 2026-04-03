package com.example.concertreservation.global.error.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PerformanceErrorCode implements ErrorCode {

    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 공연입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
