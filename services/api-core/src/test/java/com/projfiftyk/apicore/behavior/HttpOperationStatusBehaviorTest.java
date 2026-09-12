package com.projfiftyk.apicore.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projfiftyk.apicore.engine.OperationResult;
import com.projfiftyk.apicore.engine.Payload;
import com.projfiftyk.apicore.engine.expression.JmesPathExpressionResolver;
import com.projfiftyk.apicore.engine.http.HttpMethod;
import com.projfiftyk.apicore.engine.http.HttpOperation;
import com.projfiftyk.apicore.engine.http.HttpTemplateConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A living spec (Given/When/Then) for what makes an HTTP step "successful": the HTTP
 * response's status code, not merely whether a response body could be parsed. A 404
 * with a well-formed JSON error body is still a failed step, not a successful one.
 */
@DisplayName("An HttpOperation's success")
class HttpOperationStatusBehaviorTest {

    private HttpServer server;
    private int port;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/ok", exchange -> respond(exchange, 200, "{\"status\":\"ok\"}"));
        server.createContext("/not-found", exchange -> respond(exchange, 404, "{\"error\":\"not found\"}"));
        server.createContext("/unavailable", exchange -> respond(exchange, 503, "{\"error\":\"try again later\"}"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private OperationResult callPath(String path) {
        HttpTemplateConfig config = new HttpTemplateConfig(
                HttpMethod.GET, "http://localhost:" + port + path, List.of(), null);
        HttpOperation operation = new HttpOperation(config, new JmesPathExpressionResolver(),
                HttpClient.newHttpClient(), objectMapper);
        return operation.run(Payload.empty());
    }

    @Nested
    @DisplayName("given the remote API returns 2xx")
    class GivenA2xxResponse {

        @Test
        @DisplayName("when the step runs, then it is reported as successful and the body becomes the output payload")
        void isSuccessful() {
            OperationResult result = callPath("/ok");

            assertThat(result.success()).isTrue();
            assertThat(result.payload().value().get("status").asText()).isEqualTo("ok");
        }
    }

    @Nested
    @DisplayName("given the remote API returns a 4xx error, even with a well-formed JSON body")
    class GivenA4xxResponse {

        @Test
        @DisplayName("when the step runs, then it is reported as failed, not successful")
        void isReportedAsFailure() {
            OperationResult result = callPath("/not-found");

            assertThat(result.success()).isFalse();
            assertThat(result.error()).contains("404");
        }

        @Test
        @DisplayName("when the step runs, then a chain built on it would correctly stop, since a 404 is not treated as \"fine, moving on\"")
        void doesNotMasqueradeAsSuccess() {
            OperationResult result = callPath("/not-found");

            // this is exactly the bug being guarded against: a 404 must never satisfy result.success()
            assertThat(result.success()).isNotEqualTo(true);
        }
    }

    @Nested
    @DisplayName("given the remote API returns a 5xx error")
    class GivenA5xxResponse {

        @Test
        @DisplayName("when the step runs, then it is reported as failed")
        void isReportedAsFailure() {
            OperationResult result = callPath("/unavailable");

            assertThat(result.success()).isFalse();
            assertThat(result.error()).contains("503");
        }
    }
}
