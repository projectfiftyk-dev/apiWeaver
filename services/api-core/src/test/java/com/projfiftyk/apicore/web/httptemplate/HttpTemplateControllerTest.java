package com.projfiftyk.apicore.web.httptemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projfiftyk.apicore.config.JacksonConfig;
import com.projfiftyk.apicore.engine.http.HttpMethod;
import com.projfiftyk.apicore.services.common.NotFoundException;
import com.projfiftyk.apicore.services.httptemplate.HttpTemplateService;
import com.projfiftyk.apicore.transfer.httptemplate.request.HttpTemplateRequest;
import com.projfiftyk.apicore.transfer.httptemplate.response.HttpTemplateResponse;
import com.projfiftyk.apicore.web.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice (controller only) test: the service layer is mocked, so this exercises
 * request mapping, validation, status codes, and error handling — not persistence
 * or the engine. See {@code integration} for the full-stack equivalent.
 */
@WebMvcTest(HttpTemplateController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class HttpTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HttpTemplateService service;

    @Test
    void createReturns201WithTheCreatedTemplate() throws Exception {
        HttpTemplateResponse response = new HttpTemplateResponse(
                1L, "Get product", null, 1, HttpMethod.GET, "https://example.com", List.of(), null, null);
        when(service.create(any())).thenReturn(response);

        HttpTemplateRequest request = new HttpTemplateRequest(
                "Get product", null, HttpMethod.GET, "https://example.com", null, null, null);

        mockMvc.perform(post("/http-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Get product"));
    }

    @Test
    void createRejectsAMissingNameWithoutCallingTheService() throws Exception {
        HttpTemplateRequest blankName = new HttpTemplateRequest(
                "", null, HttpMethod.GET, "https://example.com", null, null, null);

        mockMvc.perform(post("/http-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankName)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void findByIdReturns404WhenTheServiceReportsNotFound() throws Exception {
        when(service.findById(42L)).thenThrow(new NotFoundException("HttpTemplate not found: 42"));

        mockMvc.perform(get("/http-templates/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("HttpTemplate not found: 42"));
    }

    @Test
    void findAllReturnsWhateverTheServiceProvides() throws Exception {
        HttpTemplateResponse a = new HttpTemplateResponse(1L, "A", null, 1, HttpMethod.GET, "u", List.of(), null, null);
        HttpTemplateResponse b = new HttpTemplateResponse(2L, "B", null, 1, HttpMethod.POST, "u", List.of(), null, null);
        when(service.findAll()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/http-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[1].name").value("B"));
    }

    @Test
    void updateDelegatesToTheServiceWithThePathId() throws Exception {
        HttpTemplateResponse response = new HttpTemplateResponse(
                7L, "Renamed", null, 2, HttpMethod.PUT, "https://example.com", List.of(), null, null);
        when(service.update(eq(7L), any())).thenReturn(response);

        HttpTemplateRequest request = new HttpTemplateRequest(
                "Renamed", null, HttpMethod.PUT, "https://example.com", null, null, null);

        mockMvc.perform(put("/http-templates/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/http-templates/3"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(3L);
    }
}
