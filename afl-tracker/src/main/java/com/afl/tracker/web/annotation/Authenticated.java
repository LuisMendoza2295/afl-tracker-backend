package com.afl.tracker.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * Annotation to mark endpoints that require OAuth authentication.
 * When applied to a method or class, the AuthenticationFilter will validate
 * the Bearer token from the Authorization header.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Authenticated {
}
