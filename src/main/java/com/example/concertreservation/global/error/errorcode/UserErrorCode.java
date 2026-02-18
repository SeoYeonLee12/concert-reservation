package com.example.concertreservation.global.error.errorcode;

import static com.example.concertreservation.user.domain.Password.HASHED_ALGORITHM;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    INVALID_PASSWORD_ALGORITHM(HttpStatus.INTERNAL_SERVER_ERROR, "U001",
            HASHED_ALGORITHM + "암호화 중 오류 발생"),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "U002", "이미 가입된 이메일입니다."),
    INVALID_USERNAME_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "잘못된 아이디 혹은 비밀번호 입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U004", "존재하지 않는 유저입니다."),
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "U005", "충전 금액은 0보다 커야합니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
