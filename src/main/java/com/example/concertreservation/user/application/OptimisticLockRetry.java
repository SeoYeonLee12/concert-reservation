package com.example.concertreservation.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OptimisticLockRetry {

    private final UserService userService;

    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 1000,
            backoff = @Backoff(100)
    )
    public Long chargePointWithRetry(Long userId, Long addedPoint) {
        return userService.chargedPointWithOptimisticLock(userId, addedPoint);
    }
}
