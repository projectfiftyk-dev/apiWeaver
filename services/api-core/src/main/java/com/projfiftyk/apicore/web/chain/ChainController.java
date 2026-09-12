package com.projfiftyk.apicore.web.chain;

import com.projfiftyk.apicore.services.chain.ChainExecutionService;
import com.projfiftyk.apicore.services.chain.ChainService;
import com.projfiftyk.apicore.transfer.chain.request.ChainRequest;
import com.projfiftyk.apicore.transfer.chain.response.ChainResponse;
import com.projfiftyk.apicore.transfer.chain.response.ChainRunResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chains")
public class ChainController {

    private final ChainService chainService;
    private final ChainExecutionService chainExecutionService;

    public ChainController(ChainService chainService, ChainExecutionService chainExecutionService) {
        this.chainService = chainService;
        this.chainExecutionService = chainExecutionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChainResponse create(@Valid @RequestBody ChainRequest request) {
        return chainService.create(request);
    }

    @GetMapping
    public List<ChainResponse> findAll() {
        return chainService.findAll();
    }

    @GetMapping("/{id}")
    public ChainResponse findById(@PathVariable Long id) {
        return chainService.findById(id);
    }

    @PutMapping("/{id}")
    public ChainResponse update(@PathVariable Long id, @Valid @RequestBody ChainRequest request) {
        return chainService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        chainService.delete(id);
    }

    @PostMapping("/{id}/run")
    public ChainRunResponse run(@PathVariable Long id) {
        return chainExecutionService.run(id);
    }
}
