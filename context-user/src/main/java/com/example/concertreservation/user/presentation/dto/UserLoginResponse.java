package com.example.concertreservation.user.presentation.dto;

import com.example.concertreservation.user.application.result.UserLoginResult;

public record UserLoginResponse(
        Long userId,
        String accessToken,
        String refreshToken
) {
    public static UserLoginResponse from(UserLoginResult result) {
        return new UserLoginResponse(
                result.userId(),
                result.accessToken(),
                result.refreshToken()
        );
    }
}
