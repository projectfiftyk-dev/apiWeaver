package com.projfiftyk.apicore.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projfiftyk.apicore.engine.http.HttpMethod;
import com.projfiftyk.apicore.transfer.chain.request.ChainRequest;
import com.projfiftyk.apicore.transfer.chain.request.ChainStepRequest;
import com.projfiftyk.apicore.transfer.chain.response.ChainResponse;
import com.projfiftyk.apicore.transfer.chain.response.ChainRunResponse;
import com.projfiftyk.apicore.transfer.httptemplate.request.HttpTemplateRequest;
import com.projfiftyk.apicore.transfer.httptemplate.response.HttpTemplateResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Full-stack integration test for the GET -> POST worked example from architecture.md §7:
 * real Spring context, real H2 persistence, real HTTP calls against a local upstream
 * server. Exercises the whole vertical slice, not just one layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChainExecutionIntegrationTest {

    private static HttpServer upstream;
    private static int upstreamPort;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void startUpstream() throws IOException {
        upstream = HttpServer.create(new InetSocketAddress(0), 0);
        upstreamPort = upstream.getAddress().getPort();

        upstream.createContext("/product/someproductid", exchange -> {
            String body = "{\"id\":\"someId\",\"name\":\"Example\",\"age\":15}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        });

        upstream.createContext("/product", exchange -> {
            byte[] received = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, received.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(received);
            }
        });

        upstream.start();
    }

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @Test
    void runsGetThenPostFeedingResponseForward() throws Exception {
        HttpTemplateResponse getTemplate = postJson("/http-templates",
                new HttpTemplateRequest("Get product", null, HttpMethod.GET,
                        "http://localhost:" + upstreamPort + "/product/someproductid", null, null, null),
                HttpTemplateResponse.class);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", "{{name}}");
        body.put("echoAge", "{{age}}");
        HttpTemplateResponse postTemplate = postJson("/http-templates",
                new HttpTemplateRequest("Create product", null, HttpMethod.POST,
                        "http://localhost:" + upstreamPort + "/product", null, body, null),
                HttpTemplateResponse.class);

        ChainResponse chain = postJson("/chains",
                new ChainRequest("Get then post", null, List.of(
                        new ChainStepRequest(getTemplate.id(), 0),
                        new ChainStepRequest(postTemplate.id(), 1)
                )),
                ChainResponse.class);

        ChainRunResponse runResponse = postJson("/chains/" + chain.id() + "/run", null, ChainRunResponse.class);

        assertThat(runResponse.steps()).hasSize(2);
        assertThat(runResponse.steps().get(0).success()).isTrue();
        assertThat(runResponse.steps().get(1).success()).isTrue();
        assertThat(runResponse.finalPayload().get("name").asText()).isEqualTo("Example");
        assertThat(runResponse.finalPayload().get("echoAge").asText()).isEqualTo("15");
    }

    private <T> T postJson(String url, Object body, Class<T> responseType) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            requestBuilder = requestBuilder.content(objectMapper.writeValueAsString(body));
        }
        String response = mockMvc.perform(requestBuilder)
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, responseType);
    }
}
