package com.example.concertreservation.global.aop.retry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface Retry {

    int maxRetries() default 1000;

    int retryDelay() default 100;
}
