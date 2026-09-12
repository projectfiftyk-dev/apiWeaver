package com.projfiftyk.apicore.domain.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projfiftyk.apicore.domain.chain.ChainStep;
import com.projfiftyk.apicore.domain.chain.ChainStepListConverter;
import com.projfiftyk.apicore.domain.httptemplate.HeaderEntry;
import com.projfiftyk.apicore.domain.httptemplate.HeaderEntryListConverter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the AttributeConverters backing the JSON columns (bodyTemplate,
 * headerTemplate, steps) — no Spring/Hibernate context needed, just the round trip.
 */
class JsonConvertersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonNodeConverterRoundTripsAnArbitraryTree() {
        JsonNodeConverter converter = new JsonNodeConverter();
        ObjectNode original = objectMapper.createObjectNode();
        original.put("name", "{{name}}");
        original.putObject("nested").put("age", 15);

        String column = converter.convertToDatabaseColumn(original);
        var restored = converter.convertToEntityAttribute(column);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void jsonNodeConverterHandlesNull() {
        JsonNodeConverter converter = new JsonNodeConverter();

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void headerEntryListConverterRoundTripsSecretFlagAndValues() {
        HeaderEntryListConverter converter = new HeaderEntryListConverter();
        List<HeaderEntry> original = List.of(
                new HeaderEntry("Accept", "application/json", false),
                new HeaderEntry("Authorization", "Bearer {{token}}", true)
        );

        String column = converter.convertToDatabaseColumn(original);
        List<HeaderEntry> restored = converter.convertToEntityAttribute(column);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void chainStepListConverterRoundTripsOrderAndTemplateId() {
        ChainStepListConverter converter = new ChainStepListConverter();
        List<ChainStep> original = List.of(new ChainStep(10L, 0), new ChainStep(20L, 1));

        String column = converter.convertToDatabaseColumn(original);
        List<ChainStep> restored = converter.convertToEntityAttribute(column);

        assertThat(restored).isEqualTo(original);
    }
}
