package com.example.concertreservation.auth;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

    
}
