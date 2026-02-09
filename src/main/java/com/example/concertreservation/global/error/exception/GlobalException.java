package com.example.concertreservation.global.error.exception;


import com.example.concertreservation.global.error.errorcode.ErrorCode;
import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {

    private final ErrorCode code;

    public GlobalException(ErrorCode code) {
        super("Exception: &s, Code: %s, Message: %s".formatted(
                code,
                code.getCode(),
                code.getMessage()
        ));
        this.code = code;
    }
}
