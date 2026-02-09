package com.example.concertreservation.global.error.response;

import com.example.concertreservation.global.error.errorcode.ErrorCode;
import java.util.Map;

public record MethodArgumentErrorResponse(
        String code,
        String message,
        Map<String, String> validateErrors
) {

    public static MethodArgumentErrorResponse from(
            ErrorCode code,
            Map<String, String> validateErrors
    ) {
        return new MethodArgumentErrorResponse(
                code.getCode(),
                code.getMessage(),
                validateErrors
        );
    }
}
