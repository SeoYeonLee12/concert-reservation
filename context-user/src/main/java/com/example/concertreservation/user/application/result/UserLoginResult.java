package com.example.concertreservation.user.application.result;

public record UserLoginResult(
        Long userId,
        String accessToken,
        String refreshToken
) {
}
