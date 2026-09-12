package com.projfiftyk.apicore.services.chain;

import com.projfiftyk.apicore.domain.chain.Chain;
import com.projfiftyk.apicore.domain.chain.ChainStep;
import com.projfiftyk.apicore.repository.chain.ChainRepository;
import com.projfiftyk.apicore.services.common.NotFoundException;
import com.projfiftyk.apicore.transfer.chain.request.ChainRequest;
import com.projfiftyk.apicore.transfer.chain.response.ChainResponse;
import com.projfiftyk.apicore.transfer.chain.response.ChainStepResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChainServiceImpl implements ChainService {

    private final ChainRepository repository;

    public ChainServiceImpl(ChainRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChainResponse create(ChainRequest request) {
        Chain chain = new Chain();
        applyRequest(chain, request);
        return toResponse(repository.save(chain));
    }

    @Override
    public List<ChainResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public ChainResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Override
    public ChainResponse update(Long id, ChainRequest request) {
        Chain chain = getOrThrow(id);
        applyRequest(chain, request);
        return toResponse(repository.save(chain));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Chain not found: " + id);
        }
        repository.deleteById(id);
    }

    private Chain getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chain not found: " + id));
    }

    private void applyRequest(Chain chain, ChainRequest request) {
        chain.setName(request.name());
        chain.setDescription(request.description());
        chain.setSteps(request.steps().stream()
                .map(s -> new ChainStep(s.templateId(), s.order()))
                .toList());
    }

    private ChainResponse toResponse(Chain chain) {
        List<ChainStepResponse> steps = chain.getSteps().stream()
                .map(s -> new ChainStepResponse(s.templateId(), s.order()))
                .toList();
        return new ChainResponse(chain.getId(), chain.getName(), chain.getDescription(), steps);
    }
}
