package com.projfiftyk.apicore.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projfiftyk.apicore.engine.http.HttpMethod;
import com.projfiftyk.apicore.transfer.httptemplate.request.HeaderEntryRequest;
import com.projfiftyk.apicore.transfer.httptemplate.request.HttpTemplateRequest;
import com.projfiftyk.apicore.transfer.httptemplate.response.HttpTemplateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack CRUD lifecycle against a real H2 database, proving the JSON-column
 * converters (headerTemplate, bodyTemplate) survive an actual persist/reload —
 * not just the in-memory round trip covered by JsonConvertersTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HttpTemplateCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createThenReadThenUpdateThenDeleteRoundTripsThroughRealPersistence() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.putObject("nested").put("name", "{{name}}");
        HttpTemplateRequest createRequest = new HttpTemplateRequest(
                "Create product", "creates a product", HttpMethod.POST, "https://example.com/products",
                List.of(new HeaderEntryRequest("Authorization", "Bearer secret", true)),
                body, null);

        String createJson = mockMvc.perform(post("/http-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        HttpTemplateResponse created = objectMapper.readValue(createJson, HttpTemplateResponse.class);

        assertThat(created.id()).isNotNull();
        assertThat(created.version()).isEqualTo(1);
        assertThat(created.headerTemplate().get(0).secret()).isTrue();
        assertThat(created.headerTemplate().get(0).value()).isNull(); // masked, even right after create

        String getJson = mockMvc.perform(get("/http-templates/" + created.id()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        HttpTemplateResponse reloaded = objectMapper.readValue(getJson, HttpTemplateResponse.class);

        assertThat(reloaded.bodyTemplate()).isEqualTo(body);
        assertThat(reloaded.urlTemplate()).isEqualTo("https://example.com/products");

        HttpTemplateRequest updateRequest = new HttpTemplateRequest(
                "Create product v2", null, HttpMethod.POST, "https://example.com/products/v2",
                List.of(), null, null);
        String updateJson = mockMvc.perform(put("/http-templates/" + created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        HttpTemplateResponse updated = objectMapper.readValue(updateJson, HttpTemplateResponse.class);

        assertThat(updated.version()).isEqualTo(2);
        assertThat(updated.urlTemplate()).isEqualTo("https://example.com/products/v2");

        mockMvc.perform(delete("/http-templates/" + created.id()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/http-templates/" + created.id()))
                .andExpect(status().isNotFound());
    }
}
