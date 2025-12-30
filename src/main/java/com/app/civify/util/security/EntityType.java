package com.app.civify.util.security;

/**
 * Entity types for ID encoding to prevent ID reuse attacks.
 * Each entity type has a unique prefix used in encoding.
 */
public enum EntityType {
    USER("U"),
    PLAN("P"),
    SUBSCRIPTION("S"),
    PROMO_CODE("R"),
    TOKEN_PACKAGE("T"),
    API_KEY("K"),
    JOB_APPLICATION("J"),
    GROUP("G"),
    PERMISSION("M"),
    CV("C"),
    NOTIFICATION("N"),
    INVOICE("I");

    private final String prefix;

    EntityType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
