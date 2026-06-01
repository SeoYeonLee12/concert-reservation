package com.example.concertreservation.reservation.application.strategy;

import com.example.concertreservation.global.error.errorcode.ReservationErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NamedLockService {

    private static final int LOCK_TIMEOUT_SEC = 3;

    // 필드명이 빈 이름과 일치 → Spring이 namedLockJdbcTemplate 빈을 주입 (타입 충돌 시 이름으로 disambiguate)
    private final JdbcTemplate namedLockJdbcTemplate;

    public void getLock(String lockKey) {
        Integer result = namedLockJdbcTemplate.queryForObject(
                "SELECT GET_LOCK(?, ?)", Integer.class, lockKey, LOCK_TIMEOUT_SEC);
        if (result == null || result == 0) {
            log.warn("[Named Lock] 락 획득 실패: key={}", lockKey);
            throw new GlobalException(ReservationErrorCode.SEAT_LOCK_TIMEOUT);
        }
        log.debug("[Named Lock] 락 획득 성공: key={}", lockKey);
    }

    public void releaseLock(String lockKey) {
        namedLockJdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, lockKey);
        log.debug("[Named Lock] 락 해제: key={}", lockKey);
    }
}
