package com.example.concertreservation.auth.application;

import com.example.concertreservation.auth.Token;
import com.example.concertreservation.auth.TokenErrorCode;
import com.example.concertreservation.auth.TokenProperty;
import com.example.concertreservation.auth.TokenService;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.user.application.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisService redisService;
    private final TokenService tokenService;
    private final TokenProperty tokenProperty;

    public Token reissueToken(Long userId, String refreshToken) {
        String savedRefreshToken = redisService.getRefreshToken(userId);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new GlobalException(TokenErrorCode.INVALID_TOKEN);
        }
        Token newToken = tokenService.issueTokens(userId);
        redisService.save(userId, newToken.refreshToken(), tokenProperty.refreshTokenExpirationMillis());
        return newToken;
    }

    public void deleteToken(Long userId) {
        redisService.delete(userId);
    }
}
