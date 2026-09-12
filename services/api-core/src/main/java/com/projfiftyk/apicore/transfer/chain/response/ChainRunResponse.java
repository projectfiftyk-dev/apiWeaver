package com.projfiftyk.apicore.transfer.chain.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ChainRunResponse(Long chainId, List<ChainStepResultResponse> steps, JsonNode finalPayload) {
}
