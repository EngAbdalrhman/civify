package com.app.civify.security.annotation;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CivifyAuthorization {
    String permission();

}
