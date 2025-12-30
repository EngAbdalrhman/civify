package com.app.civify.dto.common;

import com.app.civify.util.security.EntityType;
import lombok.Getter;

/**
 * Wrapper for encoded IDs with type information.
 * Used in controllers with @PathVariable for automatic decoding.
 */
@Getter
public class EncodedId {
    private final String encoded;
    private final Long decoded;
    private final EntityType entityType;

    public EncodedId(String encoded, Long decoded, EntityType entityType) {
        this.encoded = encoded;
        this.decoded = decoded;
        this.entityType = entityType;
    }

    public Long value() {
        return decoded;
    }

    @Override
    public String toString() {
        return encoded;
    }
}
