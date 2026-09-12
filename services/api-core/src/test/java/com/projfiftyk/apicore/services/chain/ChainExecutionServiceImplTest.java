package com.projfiftyk.apicore.services.chain;

import com.projfiftyk.apicore.domain.chain.Chain;
import com.projfiftyk.apicore.domain.chain.ChainStep;
import com.projfiftyk.apicore.domain.httptemplate.HttpTemplate;
import com.projfiftyk.apicore.engine.Engine;
import com.projfiftyk.apicore.engine.Operation;
import com.projfiftyk.apicore.engine.OperationResult;
import com.projfiftyk.apicore.engine.Payload;
import com.projfiftyk.apicore.engine.factory.OperationFactory;
import com.projfiftyk.apicore.engine.http.HttpTemplateConfig;
import com.projfiftyk.apicore.repository.chain.ChainRepository;
import com.projfiftyk.apicore.repository.httptemplate.HttpTemplateRepository;
import com.projfiftyk.apicore.services.common.NotFoundException;
import com.projfiftyk.apicore.services.httptemplate.HttpTemplateEngineMapper;
import com.projfiftyk.apicore.transfer.chain.response.ChainRunResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChainExecutionServiceImplTest {

    @Mock
    private ChainRepository chainRepository;

    @Mock
    private HttpTemplateRepository httpTemplateRepository;

    @Mock
    private HttpTemplateEngineMapper httpTemplateEngineMapper;

    @Mock
    private OperationFactory operationFactory;

    @Mock
    private Engine engine;

    @InjectMocks
    private ChainExecutionServiceImpl service;

    @Test
    void runThrowsNotFoundWhenChainIsMissing() {
        when(chainRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.run(1L)).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(httpTemplateRepository, operationFactory, engine);
    }

    @Test
    void runThrowsNotFoundWhenAStepsTemplateIsMissingAndNeverCallsTheEngine() {
        Chain chain = new Chain();
        chain.setId(1L);
        chain.setSteps(List.of(new ChainStep(10L, 0)));
        when(chainRepository.findById(1L)).thenReturn(Optional.of(chain));
        when(httpTemplateRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.run(1L)).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(engine);
    }

    @Test
    void runsStepsInOrderRegardlessOfHowTheyAreStoredAndMapsTheEngineResultBack() {
        Chain chain = new Chain();
        chain.setId(1L);
        // stored out of order on purpose
        chain.setSteps(List.of(new ChainStep(20L, 1), new ChainStep(10L, 0)));
        when(chainRepository.findById(1L)).thenReturn(Optional.of(chain));

        HttpTemplate templateA = new HttpTemplate();
        templateA.setId(10L);
        HttpTemplate templateB = new HttpTemplate();
        templateB.setId(20L);
        when(httpTemplateRepository.findById(10L)).thenReturn(Optional.of(templateA));
        when(httpTemplateRepository.findById(20L)).thenReturn(Optional.of(templateB));

        HttpTemplateConfig configA = new HttpTemplateConfig(null, "https://a", List.of(), null);
        HttpTemplateConfig configB = new HttpTemplateConfig(null, "https://b", List.of(), null);
        when(httpTemplateEngineMapper.toEngineConfig(templateA)).thenReturn(configA);
        when(httpTemplateEngineMapper.toEngineConfig(templateB)).thenReturn(configB);

        Operation operationA = payload -> OperationResult.ok(payload);
        Operation operationB = payload -> OperationResult.ok(payload);
        when(operationFactory.create(configA)).thenReturn(operationA);
        when(operationFactory.create(configB)).thenReturn(operationB);

        Payload finalPayload = Payload.empty();
        Engine.EngineRunResult engineResult = new Engine.EngineRunResult(List.of(
                OperationResult.ok(Payload.empty()),
                OperationResult.ok(finalPayload)
        ));
        when(engine.run(eq(List.of(operationA, operationB)), any(Payload.class))).thenReturn(engineResult);

        ChainRunResponse response = service.run(1L);

        assertThat(response.chainId()).isEqualTo(1L);
        assertThat(response.steps()).hasSize(2);
        // response steps line up with the sorted (10 then 20) order, not storage order
        assertThat(response.steps().get(0).templateId()).isEqualTo(10L);
        assertThat(response.steps().get(1).templateId()).isEqualTo(20L);
        assertThat(response.steps()).allMatch(com.projfiftyk.apicore.transfer.chain.response.ChainStepResultResponse::success);
    }

    @Test
    void aFailedStepIsReflectedInTheResponseWithoutThrowing() {
        Chain chain = new Chain();
        chain.setId(1L);
        chain.setSteps(List.of(new ChainStep(10L, 0)));
        when(chainRepository.findById(1L)).thenReturn(Optional.of(chain));

        HttpTemplate template = new HttpTemplate();
        template.setId(10L);
        when(httpTemplateRepository.findById(10L)).thenReturn(Optional.of(template));

        HttpTemplateConfig config = new HttpTemplateConfig(null, "https://a", List.of(), null);
        when(httpTemplateEngineMapper.toEngineConfig(template)).thenReturn(config);

        Operation operation = payload -> OperationResult.failure("boom");
        when(operationFactory.create(config)).thenReturn(operation);

        Engine.EngineRunResult engineResult = new Engine.EngineRunResult(List.of(OperationResult.failure("boom")));
        when(engine.run(anyList(), any(Payload.class))).thenReturn(engineResult);

        ChainRunResponse response = service.run(1L);

        assertThat(response.steps()).hasSize(1);
        assertThat(response.steps().get(0).success()).isFalse();
        assertThat(response.steps().get(0).error()).isEqualTo("boom");
    }
}
