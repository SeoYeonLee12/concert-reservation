package com.example.concertreservation.auth;

import jakarta.validation.constraintvalidation.SupportedValidationTarget;
import org.hibernate.annotations.Target;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
}
