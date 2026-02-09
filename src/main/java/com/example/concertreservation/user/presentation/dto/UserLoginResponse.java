package com.example.concertreservation.user.presentation.dto;

public record UserLoginResponse(
        Long userId,
        String accessToken

) {

}
