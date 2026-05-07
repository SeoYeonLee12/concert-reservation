package com.example.concertreservation.auth;

import com.example.concertreservation.global.error.errorcode.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TokenErrorCode implements ErrorCode {

    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "TE001", "만료된 토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "TE002", "올바르지 않은 토큰 형식입니다."),
    UNKNOWN_TOKEN(HttpStatus.INTERNAL_SERVER_ERROR, "TE003", "예기치 못한 토큰 에러가 발생했습니다."),
    REQUIRED_BEARER_TOKEN(HttpStatus.UNAUTHORIZED, "TE004", "Bearer 토큰 예외가 발생했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
