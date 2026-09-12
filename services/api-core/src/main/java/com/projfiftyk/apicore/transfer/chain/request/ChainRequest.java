package com.projfiftyk.apicore.transfer.chain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChainRequest(
        @NotBlank String name,
        String description,
        @NotEmpty List<ChainStepRequest> steps
) {
}
