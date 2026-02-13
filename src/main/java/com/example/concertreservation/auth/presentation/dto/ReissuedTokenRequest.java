package com.example.concertreservation.auth.presentation.dto;

import jakarta.validation.constraints.NotEmpty;

public record ReissuedTokenRequest(
        @NotEmpty(message = "토큰 값을 입력하세요.")
        String refreshToken
) {

}
