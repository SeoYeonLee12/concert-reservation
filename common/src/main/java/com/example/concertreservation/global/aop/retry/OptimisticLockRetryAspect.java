package com.example.concertreservation.global.aop.retry;

import jakarta.persistence.OptimisticLockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.StaleObjectStateException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Order(value = Ordered.LOWEST_PRECEDENCE - 1)
@Aspect
@Component
public class OptimisticLockRetryAspect {

    @Around("@annotation(retry)")
    public Object retryOptimisticLock(ProceedingJoinPoint joinPoint, Retry retry) throws Throwable {
        Exception exceptionHolder = null;

        int maxRetries = retry.maxRetries();
        int retryDelay = retry.retryDelay();

        for (int i = 0; i < maxRetries; i++) {
            try {
                return joinPoint.proceed();
            } catch (
                    ObjectOptimisticLockingFailureException |
                    OptimisticLockException |
                    StaleObjectStateException e
            ) {
                exceptionHolder = e;
                Thread.sleep(retryDelay);
            }
        }

        assert exceptionHolder != null;
        throw exceptionHolder;
    }
}
