package com.projfiftyk.apicore.transfer.chain.response;

import java.util.List;

public record ChainResponse(Long id, String name, String description, List<ChainStepResponse> steps) {
}
