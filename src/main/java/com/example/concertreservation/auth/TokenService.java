package com.example.concertreservation.auth;

import com.example.concertreservation.global.error.exception.GlobalException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenService {

    private static final String USER_ID_CLAIM = "userId";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public TokenService(TokenProperty tokenProperty) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(tokenProperty.secretKey()));
        this.accessTokenExpirationMillis = tokenProperty.accessTokenExpirationMillis();
        this.refreshTokenExpirationMillis = tokenProperty.refreshTokenExpirationMillis();
    }

    public Token issueTokens(Long userId) {
        String accessToken = createAccessToken(userId);
        String refreshToken = createRefreshToken(userId);
        return new Token(accessToken, refreshToken);
    }

    public Token reissueAccessToken(Long userId) {
        String accessToken = createAccessToken(userId);
        return new Token(accessToken);
    }

    public Long extractUserId(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(USER_ID_CLAIM, Long.class);
        } catch (ExpiredJwtException e) {
            throw new GlobalException(TokenErrorCode.EXPIRED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new GlobalException(TokenErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            throw new GlobalException(TokenErrorCode.UNKNOWN_TOKEN);
        }
    }

    private String createAccessToken(Long userId) {
        return Jwts.builder()
                .claim(USER_ID_CLAIM, userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMillis))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    private String createRefreshToken(Long userId) {
        return Jwts.builder()
                .claim(USER_ID_CLAIM, userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMillis))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
}
