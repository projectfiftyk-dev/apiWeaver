package com.projfiftyk.apicore.domain.httptemplate;

/**
 * One header entry on a persisted {@link HttpTemplate}. {@code secret} headers are
 * masked in responses; encryption at rest is deferred (see architecture.md decisions log).
 */
public record HeaderEntry(String name, String value, boolean secret) {
}
