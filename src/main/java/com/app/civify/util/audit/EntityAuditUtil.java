package com.app.civify.util.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting entities to Maps for audit logging
 * Excludes sensitive fields and handles nested objects
 */
@Slf4j
public class EntityAuditUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert entity to Map, excluding sensitive fields
     */
    public static Map<String, Object> entityToMap(Object entity, String... excludeFields) {
        if (entity == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = entity.getClass();

        // Get all fields including inherited ones
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    String fieldName = field.getName();

                    // Skip excluded fields
                    if (shouldExcludeField(fieldName, excludeFields)) {
                        continue;
                    }

                    Object value = field.get(entity);

                    // Convert value to serializable format
                    if (value != null) {
                        map.put(fieldName, convertValue(value));
                    }
                } catch (Exception e) {
                    log.debug("Could not access field {}: {}", field.getName(), e.getMessage());
                }
            }
            clazz = clazz.getSuperclass();
        }

        return map;
    }

    /**
     * Check if field should be excluded
     */
    private static boolean shouldExcludeField(String fieldName, String... excludeFields) {
        // Always exclude these sensitive fields
        String[] alwaysExclude = {"password", "apiKeyHash", "oauthSubject", "serialVersionUID"};
        
        for (String exclude : alwaysExclude) {
            if (fieldName.equals(exclude)) {
                return true;
            }
        }

        // Check custom exclude list
        if (excludeFields != null) {
            for (String exclude : excludeFields) {
                if (fieldName.equals(exclude)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Convert value to serializable format
     */
    private static Object convertValue(Object value) {
        if (value == null) {
            return null;
        }

        // Primitive types and strings
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        // Enums
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }

        // Dates
        if (value instanceof java.time.LocalDateTime || value instanceof java.time.LocalDate) {
            return value.toString();
        }

        // Collections - skip to avoid circular references
        if (value instanceof java.util.Collection || value instanceof java.util.Map) {
            return "[Collection]";
        }

        // Other objects - try to get ID or toString
        try {
            Field idField = value.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object id = idField.get(value);
            return id != null ? id.toString() : value.toString();
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * Get entity ID as String
     */
    public static String getEntityId(Object entity) {
        if (entity == null) {
            return null;
        }

        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object id = idField.get(entity);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            // Try superclass
            try {
                Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                Object id = idField.get(entity);
                return id != null ? id.toString() : null;
            } catch (Exception ex) {
                log.debug("Could not get entity ID: {}", ex.getMessage());
                return null;
            }
        }
    }

    /**
     * Get entity type name
     */
    public static String getEntityType(Object entity) {
        if (entity == null) {
            return null;
        }
        return entity.getClass().getSimpleName();
    }
}

