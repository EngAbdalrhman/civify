package com.app.civify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the required ApiKey scope to invoke an external endpoint.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresScope {
    /**
     * Required scope string (e.g. "cv:tailor", "cv:parse", "apps:write").
     */
    String value();
}
