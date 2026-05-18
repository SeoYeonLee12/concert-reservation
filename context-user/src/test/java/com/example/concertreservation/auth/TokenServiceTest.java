package com.example.concertreservation.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private static final String SECRET_KEY =
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RpbmctcHVycG9zZXMtb25seQ==";
    private static final long ACCESS_TTL = 3_600_000L;   // 1 hour
    private static final long REFRESH_TTL = 86_400_000L; // 24 hours

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        TokenProperty property = new TokenProperty(SECRET_KEY, ACCESS_TTL, REFRESH_TTL);
        tokenService = new TokenService(property);
    }

    @Test
    void extractJti_액세스토큰에서_jti_추출() {
        Token token = tokenService.issueTokens(1L);
        String jti = tokenService.extractJti(token.accessToken());
        assertThat(jti).isNotBlank();
    }

    @Test
    void extractJti_액세스토큰과_리프레시토큰_jti_다름() {
        Token token = tokenService.issueTokens(1L);
        String accessJti = tokenService.extractJti(token.accessToken());
        String refreshJti = tokenService.extractJti(token.refreshToken());
        assertThat(accessJti).isNotEqualTo(refreshJti);
    }

    @Test
    void remainingTtlMillis_만료전_양수반환() {
        Token token = tokenService.issueTokens(1L);
        long remaining = tokenService.remainingTtlMillis(token.accessToken());
        assertThat(remaining).isGreaterThan(0L).isLessThanOrEqualTo(ACCESS_TTL);
    }
}
