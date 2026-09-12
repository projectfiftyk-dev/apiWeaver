package com.projfiftyk.apicore.engine.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projfiftyk.apicore.engine.OperationResult;
import com.projfiftyk.apicore.engine.Payload;
import com.projfiftyk.apicore.engine.expression.JmesPathExpressionResolver;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HttpOperationTest {

    private HttpServer server;
    private int port;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> capturedContentType = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/echo", exchange -> {
            capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] received = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, received.length == 0 ? -1 : received.length);
            if (received.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(received);
                }
            }
        });
        server.createContext("/created", exchange -> respondWithStatus(exchange, 201, "{\"id\":1}"));
        server.createContext("/not-found", exchange -> respondWithStatus(exchange, 404, "{\"error\":\"no such resource\"}"));
        server.createContext("/server-error", exchange -> respondWithStatus(exchange, 500, "{\"error\":\"boom\"}"));
        server.start();
    }

    private void respondWithStatus(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    /**
     * Regression test: HttpOperation used to send a body with no Content-Type header
     * at all, which real APIs (e.g. jsonplaceholder.typicode.com) silently ignore —
     * they parse nothing and just return a fake id, never the submitted fields.
     */
    @Test
    void defaultsContentTypeToApplicationJsonWhenABodyIsPresent() {
        HttpTemplateConfig config = new HttpTemplateConfig(
                HttpMethod.POST, "http://localhost:" + port + "/echo", List.of(),
                objectMapper.createObjectNode().put("name", "Example"));
        HttpOperation operation = new HttpOperation(config, new JmesPathExpressionResolver(),
                HttpClient.newHttpClient(), objectMapper);

        OperationResult result = operation.run(Payload.empty());

        assertThat(result.success()).isTrue();
        assertThat(capturedContentType.get()).isEqualTo("application/json");
        assertThat(result.payload().value().get("name").asText()).isEqualTo("Example");
    }

    @Test
    void respectsAnExplicitlyTemplatedContentTypeHeader() {
        HttpTemplateConfig config = new HttpTemplateConfig(
                HttpMethod.POST, "http://localhost:" + port + "/echo",
                List.of(new HeaderConfig("Content-Type", "application/vnd.custom+json", false)),
                objectMapper.createObjectNode().put("name", "Example"));
        HttpOperation operation = new HttpOperation(config, new JmesPathExpressionResolver(),
                HttpClient.newHttpClient(), objectMapper);

        operation.run(Payload.empty());

        assertThat(capturedContentType.get()).isEqualTo("application/vnd.custom+json");
    }

    @Test
    void setsNoContentTypeWhenThereIsNoBody() {
        HttpTemplateConfig config = new HttpTemplateConfig(
                HttpMethod.GET, "http://localhost:" + port + "/echo", List.of(), null);
        HttpOperation operation = new HttpOperation(config, new JmesPathExpressionResolver(),
                HttpClient.newHttpClient(), objectMapper);

        operation.run(Payload.empty());

        assertThat(capturedContentType.get()).isNull();
    }

    /**
     * Success must be determined by the HTTP response, not merely by "we got a
     * parseable body back" — a 404/500 with a valid JSON error body used to be
     * reported as a successful step.
     */
    @Test
    void a2xxResponseIsReportedAsSuccess() {
        HttpTemplateConfig config = new HttpTemplateConfig(
                HttpMethod.POST, "http://localhost:" + port + "/created", List.of(), null);
        HttpOperation operation = new HttpOperation(config, new JmesPathExpressionResolver(),
                HttpClient.newHttpClient(), objectMapper);

        OperationResult result = operation.run(Payload.empty());

        assertThat(result.success()).isTrue();
        assertThat(result.payload().value().get("id").asInt()).isEqualTo(1);
    }

    @Test
    void a404ResponseIsReportedAsFailureNotSuccess() {
        HttpTemplateConfig config = new HttpTemplateConfig(
                HttpMethod.GET, "http://localhost:" + port + "/not-found", List.of(), null);
        HttpOperation operation = new HttpOperation(config, new JmesPathExpressionResolver(),
                HttpClient.newHttpClient(), objectMapper);

        OperationResult result = operation.run(Payload.empty());

        assertThat(result.success()).isFalse();
        assertThat(result.payload()).isNull();
        assertThat(result.error()).contains("404").contains("no such resource");
    }

    @Test
    void a500ResponseIsReportedAsFailure() {
        HttpTemplateConfig config = new HttpTemplateConfig(
                HttpMethod.GET, "http://localhost:" + port + "/server-error", List.of(), null);
        HttpOperation operation = new HttpOperation(config, new JmesPathExpressionResolver(),
                HttpClient.newHttpClient(), objectMapper);

        OperationResult result = operation.run(Payload.empty());

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("500");
    }
}
