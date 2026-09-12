package com.projfiftyk.apicore.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * The plain JSON object flowing through {@link Engine#run} at runtime.
 * Reserved vocabulary: nothing else in this codebase is ever called "Payload".
 */
public record Payload(JsonNode value) {

    public static Payload empty() {
        return new Payload(NullNode.getInstance());
    }
}
