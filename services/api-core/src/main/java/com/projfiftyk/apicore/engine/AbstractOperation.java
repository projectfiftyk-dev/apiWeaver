package com.projfiftyk.apicore.engine;

/**
 * Template Method skeleton shared by every concrete Operation: bindInput -> execute -> mapOutput.
 * Concrete subclasses only fill in the steps that vary by Operation type.
 *
 * @param <B> the type this Operation's Template config binds the incoming Payload into
 * @param <R> the raw result produced by {@link #execute}, before normalization
 */
public abstract class AbstractOperation<B, R> implements Operation {

    @Override
    public final OperationResult run(Payload payload) {
        try {
            B bound = bindInput(payload);
            R raw = execute(bound);
            Payload output = mapOutput(raw);
            return OperationResult.ok(output);
        } catch (Exception e) {
            return OperationResult.failure(e.getMessage());
        }
    }

    protected abstract B bindInput(Payload payload);

    protected abstract R execute(B bound) throws Exception;

    protected abstract Payload mapOutput(R raw);
}
