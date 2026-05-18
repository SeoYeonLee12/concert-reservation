package com.example.concertreservation.global.error.errorcode;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
