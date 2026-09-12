package com.projfiftyk.apicore.services.chain;

import com.projfiftyk.apicore.transfer.chain.request.ChainRequest;
import com.projfiftyk.apicore.transfer.chain.response.ChainResponse;

import java.util.List;

public interface ChainService {

    ChainResponse create(ChainRequest request);

    List<ChainResponse> findAll();

    ChainResponse findById(Long id);

    ChainResponse update(Long id, ChainRequest request);

    void delete(Long id);
}
