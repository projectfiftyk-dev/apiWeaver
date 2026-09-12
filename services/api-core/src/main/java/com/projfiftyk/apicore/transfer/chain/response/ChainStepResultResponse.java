package com.projfiftyk.apicore.transfer.chain.response;

import com.fasterxml.jackson.databind.JsonNode;

public record ChainStepResultResponse(Long templateId, boolean success, JsonNode payload, String error) {
}
