package com.projfiftyk.apicore.engine.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.projfiftyk.apicore.engine.AbstractOperation;
import com.projfiftyk.apicore.engine.Payload;
import com.projfiftyk.apicore.engine.expression.ExpressionResolver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * The Operation type for HTTP calls: bindInput resolves {{expr}} placeholders against
 * the incoming Payload, execute fires the request, mapOutput normalizes the response
 * body back into a Payload.
 */
public class HttpOperation extends AbstractOperation<HttpOperation.BoundRequest, HttpResponse<String>> {

    private final HttpTemplateConfig config;
    private final ExpressionResolver expressionResolver;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpOperation(HttpTemplateConfig config, ExpressionResolver expressionResolver,
                          HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.expressionResolver = expressionResolver;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected BoundRequest bindInput(Payload payload) {
        JsonNode payloadValue = payload.value();

        String url = expressionResolver.resolve(config.urlTemplate(), payloadValue);

        Map<String, String> headers = new java.util.LinkedHashMap<>();
        if (config.headerTemplate() != null) {
            for (HeaderConfig header : config.headerTemplate()) {
                headers.put(header.name(), expressionResolver.resolve(header.value(), payloadValue));
            }
        }

        String body = null;
        if (config.bodyTemplate() != null && !config.bodyTemplate().isNull()) {
            body = resolveBody(config.bodyTemplate(), payloadValue).toString();
        }

        return new BoundRequest(url, headers, body);
    }

    @Override
    protected HttpResponse<String> execute(BoundRequest bound) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(bound.url()));
        bound.headers().forEach(builder::header);

        boolean hasExplicitContentType = bound.headers().keySet().stream()
                .anyMatch(name -> name.equalsIgnoreCase("Content-Type"));
        if (bound.body() != null && !hasExplicitContentType) {
            builder.header("Content-Type", "application/json");
        }

        HttpRequest.BodyPublisher bodyPublisher = bound.body() == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(bound.body());

        builder.method(config.method().name(), bodyPublisher);

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Override
    protected Payload mapOutput(HttpResponse<String> raw) {
        String body = raw.body();
        if (body == null || body.isBlank()) {
            return new Payload(NullNode.getInstance());
        }
        try {
            return new Payload(objectMapper.readTree(body));
        } catch (Exception e) {
            throw new IllegalStateException("Response body is not valid JSON: " + e.getMessage(), e);
        }
    }

    private JsonNode resolveBody(JsonNode node, JsonNode payload) {
        if (node.isTextual()) {
            return objectMapper.getNodeFactory().textNode(expressionResolver.resolve(node.asText(), payload));
        }
        if (node.isObject()) {
            var result = objectMapper.getNodeFactory().objectNode();
            node.fields().forEachRemaining(entry -> result.set(entry.getKey(), resolveBody(entry.getValue(), payload)));
            return result;
        }
        if (node.isArray()) {
            var result = objectMapper.getNodeFactory().arrayNode();
            for (JsonNode element : node) {
                result.add(resolveBody(element, payload));
            }
            return result;
        }
        return node;
    }

    public record BoundRequest(String url, Map<String, String> headers, String body) {
    }
}
