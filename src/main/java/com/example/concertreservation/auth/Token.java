package com.example.concertreservation.auth;

public record Token(
        String accessToken,
        String refreshToken
) {
    public Token(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public Token(String accessToken) {
        this(accessToken, null);
    }
}
