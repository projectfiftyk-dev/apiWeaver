package com.projfiftyk.apicore.transfer.httptemplate.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.projfiftyk.apicore.engine.http.HttpMethod;

import java.util.List;

public record HttpTemplateResponse(
        Long id,
        String name,
        String description,
        int version,
        HttpMethod method,
        String urlTemplate,
        List<HeaderEntryResponse> headerTemplate,
        JsonNode bodyTemplate,
        JsonNode declaredOutput
) {
}
