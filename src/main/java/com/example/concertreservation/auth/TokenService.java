package com.example.concertreservation.auth;

import com.example.concertreservation.global.error.exception.GlobalException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class TokenService {

    private static final String USER_ID_CLAIM = "userId";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMillis;

    public TokenService(TokenProperty tokenProperty) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(tokenProperty.secretKey()));
        this.accessTokenExpirationMillis = tokenProperty.accessTokenExpirationMillis();
    }

    public Token createToken(Long userId) {
        String accessToken = Jwts.builder()
                .claim(USER_ID_CLAIM, userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMillis))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
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
}
