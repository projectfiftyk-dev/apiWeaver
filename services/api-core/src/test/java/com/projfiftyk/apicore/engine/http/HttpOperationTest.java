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
        server.start();
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
}
