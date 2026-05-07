package com.example.concertreservation.user.application.result;

import com.example.concertreservation.user.domain.User;

public record UserInfoResult(
        String email,
        String name,
        String nickName,
        long point

) {
    public static UserInfoResult from(User user) {
        return new UserInfoResult(
                user.getEmail(),
                user.getName(),
                user.getNickName(),
                user.getPoint()
        );
    }
}
