package com.projfiftyk.apicore.engine;

/**
 * Result type (Either) returned by every {@link Operation} — checked with a plain
 * {@code if}, never a {@code try/catch}. Exactly one of payload/error is populated.
 */
public record OperationResult(boolean success, Payload payload, String error) {

    public static OperationResult ok(Payload payload) {
        return new OperationResult(true, payload, null);
    }

    public static OperationResult failure(String error) {
        return new OperationResult(false, null, error);
    }
}
