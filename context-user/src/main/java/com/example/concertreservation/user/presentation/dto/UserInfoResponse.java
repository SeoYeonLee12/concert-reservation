package com.example.concertreservation.user.presentation.dto;

import com.example.concertreservation.user.application.result.UserInfoResult;

public record UserInfoResponse(
        String email,
        String name,
        String nickName,
        long point
) {
    public static UserInfoResponse from(UserInfoResult result) {
        return new UserInfoResponse(
                result.email(),
                result.name(),
                result.nickName(),
                result.point()
        );
    }
}
