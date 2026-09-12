package com.projfiftyk.apicore.services.chain;

import com.projfiftyk.apicore.domain.chain.Chain;
import com.projfiftyk.apicore.domain.chain.ChainStep;
import com.projfiftyk.apicore.domain.httptemplate.HttpTemplate;
import com.projfiftyk.apicore.engine.Engine;
import com.projfiftyk.apicore.engine.Operation;
import com.projfiftyk.apicore.engine.OperationResult;
import com.projfiftyk.apicore.engine.Payload;
import com.projfiftyk.apicore.engine.factory.OperationFactory;
import com.projfiftyk.apicore.repository.chain.ChainRepository;
import com.projfiftyk.apicore.repository.httptemplate.HttpTemplateRepository;
import com.projfiftyk.apicore.services.common.NotFoundException;
import com.projfiftyk.apicore.services.httptemplate.HttpTemplateEngineMapper;
import com.projfiftyk.apicore.transfer.chain.response.ChainRunResponse;
import com.projfiftyk.apicore.transfer.chain.response.ChainStepResultResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * The run path: loads a Chain and its HttpTemplates, maps each into the engine's
 * plain-Java config via {@link HttpTemplateEngineMapper}, builds Operations through
 * the {@link OperationFactory}, and runs them through the {@link Engine}.
 */
@Service
public class ChainExecutionServiceImpl implements ChainExecutionService {

    private final ChainRepository chainRepository;
    private final HttpTemplateRepository httpTemplateRepository;
    private final HttpTemplateEngineMapper httpTemplateEngineMapper;
    private final OperationFactory operationFactory;
    private final Engine engine;

    public ChainExecutionServiceImpl(ChainRepository chainRepository,
                                      HttpTemplateRepository httpTemplateRepository,
                                      HttpTemplateEngineMapper httpTemplateEngineMapper,
                                      OperationFactory operationFactory,
                                      Engine engine) {
        this.chainRepository = chainRepository;
        this.httpTemplateRepository = httpTemplateRepository;
        this.httpTemplateEngineMapper = httpTemplateEngineMapper;
        this.operationFactory = operationFactory;
        this.engine = engine;
    }

    @Override
    public ChainRunResponse run(Long chainId) {
        Chain chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new NotFoundException("Chain not found: " + chainId));

        List<ChainStep> orderedSteps = chain.getSteps().stream()
                .sorted(Comparator.comparingInt(ChainStep::order))
                .toList();

        List<Operation> operations = orderedSteps.stream()
                .map(this::buildOperation)
                .toList();

        Engine.EngineRunResult runResult = engine.run(operations, Payload.empty());

        List<ChainStepResultResponse> stepResponses = new java.util.ArrayList<>();
        List<OperationResult> stepResults = runResult.stepResults();
        for (int i = 0; i < stepResults.size(); i++) {
            OperationResult result = stepResults.get(i);
            Long templateId = orderedSteps.get(i).templateId();
            stepResponses.add(new ChainStepResultResponse(
                    templateId,
                    result.success(),
                    result.success() ? result.payload().value() : null,
                    result.error()
            ));
        }

        return new ChainRunResponse(chain.getId(), stepResponses, runResult.finalPayload().value());
    }

    private Operation buildOperation(ChainStep step) {
        HttpTemplate template = httpTemplateRepository.findById(step.templateId())
                .orElseThrow(() -> new NotFoundException("HttpTemplate not found: " + step.templateId()));
        return operationFactory.create(httpTemplateEngineMapper.toEngineConfig(template));
    }
}
