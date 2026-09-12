package com.projfiftyk.apicore.transfer.httptemplate.request;

import jakarta.validation.constraints.NotBlank;

public record HeaderEntryRequest(@NotBlank String name, String value, boolean secret) {
}
