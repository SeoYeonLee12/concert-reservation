package com.example.concertreservation.global.error.handler;

import com.example.concertreservation.global.error.errorcode.ErrorCode;
import com.example.concertreservation.global.error.errorcode.InternalServerErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.global.error.response.ErrorResponse;
import com.example.concertreservation.global.error.response.MethodArgumentErrorResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = GlobalException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(GlobalException e) {
        ErrorCode code = e.getCode();
        log.info("Error Occured: Status {}, code {}, message {}", code.getHttpStatus(),
                code.getCode(),
                code.getMessage());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ErrorResponse.from(code));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MethodArgumentErrorResponse> handleMethodArgumentException(
            MethodArgumentNotValidException error
    ) {
        BindingResult bindingResult = error.getBindingResult();

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.info("MethodArgumentNotValidException Occured: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(MethodArgumentErrorResponse
                        .from(InternalServerErrorCode.INVALID_INPUT_VALUE, errors));
    }
}
