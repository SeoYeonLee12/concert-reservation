package com.example.concertreservation.auth;

import com.example.concertreservation.global.error.exception.GlobalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class BearerTokenExtractor {

    private static final String BEARER_TOKEN = "Bearer ";
    private static final int BEGIN_INDEX_PREFIX = 7;

    public String extract(String bearerToken) {
        log.info("Bearer 토큰 추출 중...");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_TOKEN)) {
            String token = bearerToken.substring(BEGIN_INDEX_PREFIX);
            log.info("Bearer Token 추출 완료");
            return token;
        }
        throw new GlobalException(TokenErrorCode.REQUIRED_BEARER_TOKEN);
    }
}
