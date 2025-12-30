package com.app.civify.util.security;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Jackson serializer that encodes Long IDs to secure strings.
 * Use with @JsonSerialize(using = EncodedIdSerializer.class) on ID fields.
 */
@Slf4j
@Component
public class EncodedIdSerializer extends JsonSerializer<Long> implements ContextualSerializer {

    private static IdEncodingUtil staticIdEncodingUtil;
    private EntityType entityType;

    public EncodedIdSerializer() {
        this.entityType = EntityType.USER; // Default
    }

    public EncodedIdSerializer(EntityType entityType) {
        this.entityType = entityType;
    }

    @Autowired
    public void setIdEncodingUtil(IdEncodingUtil idEncodingUtil) {
        EncodedIdSerializer.staticIdEncodingUtil = idEncodingUtil;
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (staticIdEncodingUtil == null) {
            log.warn("IdEncodingUtil not initialized, writing raw ID");
            gen.writeNumber(value);
            return;
        }

        try {
            String encoded = staticIdEncodingUtil.encode(value, entityType);
            gen.writeString(encoded);
        } catch (Exception e) {
            log.error("Failed to encode ID {}: {}", value, e.getMessage());
            gen.writeNumber(value); // Fallback to raw ID
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property != null) {
            EncodedIdField annotation = property.getAnnotation(EncodedIdField.class);
            if (annotation != null) {
                return new EncodedIdSerializer(annotation.value());
            }
        }
        return this;
    }
}
