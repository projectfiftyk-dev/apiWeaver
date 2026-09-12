package com.projfiftyk.apicore.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projfiftyk.apicore.engine.Engine;
import com.projfiftyk.apicore.engine.Operation;
import com.projfiftyk.apicore.engine.OperationResult;
import com.projfiftyk.apicore.engine.Payload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A living spec (Given/When/Then) for the Engine's core contract, per architecture.md
 * §5: run operations in sequence, feed each result forward, stop at the first failure.
 */
@DisplayName("The Engine")
class EngineBehaviorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Engine engine = new Engine();

    private Payload jsonPayload(String json) {
        try {
            return new Payload(objectMapper.readTree(json));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("given every operation in the chain succeeds")
    class GivenEveryOperationSucceeds {

        @Test
        @DisplayName("when run, then each step's output payload becomes the next step's input")
        void feedsOutputForward() {
            Operation addName = payload -> {
                var node = ((com.fasterxml.jackson.databind.node.ObjectNode) payload.value().deepCopy());
                node.put("name", "Example");
                return OperationResult.ok(new Payload(node));
            };
            Operation addAge = payload -> {
                var node = ((com.fasterxml.jackson.databind.node.ObjectNode) payload.value().deepCopy());
                node.put("age", 15);
                return OperationResult.ok(new Payload(node));
            };

            Engine.EngineRunResult result = engine.run(List.of(addName, addAge), jsonPayload("{}"));

            assertThat(result.success()).isTrue();
            assertThat(result.finalPayload().value().get("name").asText()).isEqualTo("Example");
            assertThat(result.finalPayload().value().get("age").asInt()).isEqualTo(15);
        }

        @Test
        @DisplayName("when run, then the trace contains one result per operation")
        void tracesEveryStep() {
            Operation ok = payload -> OperationResult.ok(payload);

            Engine.EngineRunResult result = engine.run(List.of(ok, ok, ok), Payload.empty());

            assertThat(result.stepResults()).hasSize(3);
            assertThat(result.stepResults()).allMatch(OperationResult::success);
        }
    }

    @Nested
    @DisplayName("given an operation in the middle of the chain fails")
    class GivenAMiddleOperationFails {

        @Test
        @DisplayName("when run, then the run stops and operations after the failure never execute")
        void stopsAtFirstFailure() {
            AtomicBoolean thirdOperationRan = new AtomicBoolean(false);
            Operation ok = payload -> OperationResult.ok(payload);
            Operation failing = payload -> OperationResult.failure("upstream returned 500");
            Operation shouldNeverRun = payload -> {
                thirdOperationRan.set(true);
                return OperationResult.ok(payload);
            };

            Engine.EngineRunResult result = engine.run(List.of(ok, failing, shouldNeverRun), Payload.empty());

            assertThat(result.success()).isFalse();
            assertThat(thirdOperationRan).isFalse();
            assertThat(result.stepResults()).hasSize(2);
        }

        @Test
        @DisplayName("when run, then the failure's error message is preserved in the trace")
        void preservesTheErrorMessage() {
            Operation failing = payload -> OperationResult.failure("connection refused");

            Engine.EngineRunResult result = engine.run(List.of(failing), Payload.empty());

            assertThat(result.stepResults().get(0).error()).isEqualTo("connection refused");
            assertThat(result.finalPayload()).isEqualTo(Payload.empty());
        }
    }

    @Nested
    @DisplayName("given an empty list of operations")
    class GivenNoOperations {

        @Test
        @DisplayName("when run, then it reports failure with no steps rather than throwing")
        void reportsNoStepsWithoutThrowing() {
            Engine.EngineRunResult result = engine.run(List.of(), Payload.empty());

            assertThat(result.stepResults()).isEmpty();
            assertThat(result.success()).isFalse();
        }
    }
}
