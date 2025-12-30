package com.app.civify.util.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark Long ID fields for automatic encoding when serialized to JSON.
 * Use with @JsonSerialize(using = EncodedIdSerializer.class)
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EncodedIdField {
    /**
     * The entity type for this ID
     */
    EntityType value();
}
