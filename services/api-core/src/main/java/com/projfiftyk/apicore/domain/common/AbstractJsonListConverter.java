package com.projfiftyk.apicore.domain.common;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;

import java.util.List;

/** Base for storing a {@code List<T>} as a JSON column. Subclasses must have a no-arg constructor (JPA requirement). */
public abstract class AbstractJsonListConverter<T> implements AttributeConverter<List<T>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JavaType listType;

    protected AbstractJsonListConverter(Class<T> elementType) {
        this.listType = MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
    }

    @Override
    public String convertToDatabaseColumn(List<T> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize JSON column", e);
        }
    }

    @Override
    public List<T> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, listType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize JSON column", e);
        }
    }
}
