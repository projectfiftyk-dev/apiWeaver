package com.projfiftyk.apicore.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EngineTest {

    @Test
    void stopsAtFirstFailure() {
        Operation ok = payload -> OperationResult.ok(payload);
        Operation failing = payload -> OperationResult.failure("boom");
        Operation neverCalled = payload -> {
            throw new AssertionError("should not run after a failure");
        };

        Engine engine = new Engine();
        Engine.EngineRunResult result = engine.run(List.of(ok, failing, neverCalled), Payload.empty());

        assertThat(result.success()).isFalse();
        assertThat(result.stepResults()).hasSize(2);
        assertThat(result.stepResults().get(1).error()).isEqualTo("boom");
    }

    @Test
    void feedsEachStepsOutputIntoTheNext() {
        Operation appendA = payload -> OperationResult.ok(payload);
        Engine engine = new Engine();

        Engine.EngineRunResult result = engine.run(List.of(appendA), Payload.empty());

        assertThat(result.success()).isTrue();
        assertThat(result.finalPayload()).isEqualTo(Payload.empty());
    }
}
