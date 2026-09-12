package com.projfiftyk.apicore.web.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projfiftyk.apicore.config.JacksonConfig;
import com.projfiftyk.apicore.services.chain.ChainExecutionService;
import com.projfiftyk.apicore.services.chain.ChainService;
import com.projfiftyk.apicore.services.common.NotFoundException;
import com.projfiftyk.apicore.transfer.chain.request.ChainRequest;
import com.projfiftyk.apicore.transfer.chain.request.ChainStepRequest;
import com.projfiftyk.apicore.transfer.chain.response.ChainResponse;
import com.projfiftyk.apicore.transfer.chain.response.ChainRunResponse;
import com.projfiftyk.apicore.transfer.chain.response.ChainStepResponse;
import com.projfiftyk.apicore.transfer.chain.response.ChainStepResultResponse;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice (controller only) test for /chains: both services are mocked, so this
 * exercises request mapping, validation and error handling only.
 */
@WebMvcTest(ChainController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class ChainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChainService chainService;

    @MockitoBean
    private ChainExecutionService chainExecutionService;

    @Test
    void createRejectsAnEmptyStepListWithoutCallingTheService() throws Exception {
        ChainRequest invalid = new ChainRequest("Empty chain", null, List.of());

        mockMvc.perform(post("/chains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chainService);
    }

    @Test
    void createReturns201WithTheCreatedChain() throws Exception {
        ChainResponse response = new ChainResponse(1L, "Get then post", null,
                List.of(new ChainStepResponse(10L, 0), new ChainStepResponse(20L, 1)));
        when(chainService.create(any())).thenReturn(response);

        ChainRequest request = new ChainRequest("Get then post", null,
                List.of(new ChainStepRequest(10L, 0), new ChainStepRequest(20L, 1)));

        mockMvc.perform(post("/chains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.steps.length()").value(2));
    }

    @Test
    void runDelegatesToTheExecutionServiceAndReturnsItsTrace() throws Exception {
        ChainRunResponse runResponse = new ChainRunResponse(1L,
                List.of(new ChainStepResultResponse(10L, true, null, null)), null);
        when(chainExecutionService.run(1L)).thenReturn(runResponse);

        mockMvc.perform(post("/chains/1/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chainId").value(1))
                .andExpect(jsonPath("$.steps[0].success").value(true));
    }

    @Test
    void runReturns404WhenTheChainDoesNotExist() throws Exception {
        when(chainExecutionService.run(404L)).thenThrow(new NotFoundException("Chain not found: 404"));

        mockMvc.perform(post("/chains/404/run"))
                .andExpect(status().isNotFound());
    }
}
