package com.example.concertreservation.global.error.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InternalServerErrorCode implements ErrorCode {

    UNKNOWN_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "ISE001", "알 수 없는 예외가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ISE002", "API 호출 중 예외가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.INTERNAL_SERVER_ERROR, "ISE003", "유효하지 않은 입력값입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
