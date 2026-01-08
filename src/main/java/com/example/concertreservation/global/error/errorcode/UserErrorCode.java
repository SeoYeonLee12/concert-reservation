package com.example.concertreservation.global.error.errorcode;

import static com.example.concertreservation.user.domain.Password.HASHED_ALGORITHM;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

  INVALID_PASSWORD_ALGORITHM(HttpStatus.INTERNAL_SERVER_ERROR, "U1",
      HASHED_ALGORITHM + "암호화 중 오류 발생");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

}
