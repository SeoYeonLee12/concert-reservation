package com.example.concertreservation.global.error.response;

import com.example.concertreservation.global.error.errorcode.ErrorCode;

public record ErrorResponse(
        String code,
        String message
) {

    public static ErrorResponse from(ErrorCode code) {
        return new ErrorResponse(code.getCode(), code.getMessage());
    }
}
