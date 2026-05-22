package com.example.concertreservation.global.error.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WaitingQueueErrorCode implements ErrorCode {

    WAITING_QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "WQ001", "대기열 항목을 찾을 수 없습니다"),
    WAITING_QUEUE_NOT_ACTIVE(HttpStatus.CONFLICT, "WQ002", "아직 순번이 되지 않았습니다. 대기 중입니다"),
    WAITING_QUEUE_ALREADY_EXISTS(HttpStatus.CONFLICT, "WQ003", "이미 대기열에 등록되어 있습니다"),
    WAITING_QUEUE_INVALID_STATUS(HttpStatus.CONFLICT, "WQ004", "유효하지 않은 대기열 상태 전이입니다"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
