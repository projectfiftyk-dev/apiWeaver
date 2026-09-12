package com.projfiftyk.apicore.domain.chain;

/** One entry in a Chain's ordered list of {@code HttpTemplate} references. */
public record ChainStep(Long templateId, int order) {
}
