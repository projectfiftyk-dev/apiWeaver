package com.projfiftyk.apicore.engine.http;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Plain-Java, framework-free Template config the {@link HttpOperation} binds against.
 * This is NOT the JPA entity (see {@code domain.httptemplate.HttpTemplate}) — that
 * entity is mapped into this shape by the Spring-side seam described in architecture.md §11.
 */
public record HttpTemplateConfig(
        HttpMethod method,
        String urlTemplate,
        List<HeaderConfig> headerTemplate,
        JsonNode bodyTemplate
) {
}
