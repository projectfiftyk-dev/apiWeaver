package com.projfiftyk.apicore.engine;

/**
 * Strategy interface every Operation type honors, regardless of concrete type.
 * The Engine only ever calls {@link #run}.
 */
public interface Operation {

    OperationResult run(Payload payload);
}
