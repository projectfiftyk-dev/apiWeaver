package com.projfiftyk.apicore.services.chain;

import com.projfiftyk.apicore.transfer.chain.response.ChainRunResponse;

public interface ChainExecutionService {

    ChainRunResponse run(Long chainId);
}
