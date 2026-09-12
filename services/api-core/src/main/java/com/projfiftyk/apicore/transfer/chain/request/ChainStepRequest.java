package com.projfiftyk.apicore.transfer.chain.request;

import jakarta.validation.constraints.NotNull;

public record ChainStepRequest(@NotNull Long templateId, int order) {
}
