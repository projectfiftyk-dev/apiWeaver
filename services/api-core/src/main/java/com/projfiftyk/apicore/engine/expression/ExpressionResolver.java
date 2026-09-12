package com.projfiftyk.apicore.engine.expression;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resolves {@code {{expression}}} placeholders in a string against the current Payload,
 * via JMESPath. For MVP, always resolves to a string, even when the source is a
 * different JSON type — a known, consciously accepted limitation (see architecture.md).
 */
public interface ExpressionResolver {

    String resolve(String template, JsonNode payload);
}
