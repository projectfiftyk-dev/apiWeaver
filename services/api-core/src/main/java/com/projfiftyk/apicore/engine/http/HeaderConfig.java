package com.projfiftyk.apicore.engine.http;

/**
 * One header entry inside an {@link HttpTemplateConfig}. {@code value} is a literal,
 * a {@code {{expr}}} placeholder, or (when {@code secret} is true) never payload-derived.
 */
public record HeaderConfig(String name, String value, boolean secret) {
}
