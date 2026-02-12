package com.example.concertreservation.auth.controller.dto;

public record ReissuedTokenResponse(
        String accessToken,
        String refreshToken
) {
}






