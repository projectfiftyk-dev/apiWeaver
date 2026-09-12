package com.projfiftyk.apicore.transfer.httptemplate.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.projfiftyk.apicore.engine.http.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HttpTemplateRequest(
        @NotBlank String name,
        String description,
        @NotNull HttpMethod method,
        @NotBlank String urlTemplate,
        List<HeaderEntryRequest> headerTemplate,
        JsonNode bodyTemplate,
        JsonNode declaredOutput
) {
}
