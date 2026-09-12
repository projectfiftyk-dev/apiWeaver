package com.projfiftyk.apicore.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs an ordered list of Operations in sequence, feeding each result forward and
 * stopping at the first failure. Depends only on the {@link Operation} interface
 * (Dependency Inversion) — never on Templates, Factories, or persistence.
 */
public class Engine {

    public EngineRunResult run(List<Operation> operations, Payload seedInput) {
        Payload currentInput = seedInput;
        List<OperationResult> stepResults = new ArrayList<>();

        for (Operation operation : operations) {
            OperationResult result = operation.run(currentInput);
            stepResults.add(result);

            if (!result.success()) {
                return new EngineRunResult(stepResults);
            }

            currentInput = result.payload();
        }

        return new EngineRunResult(stepResults);
    }

    /**
     * The full per-step trace of a run, plus its outcome — enough to report exactly
     * what ran, with what data, and what it produced.
     */
    public record EngineRunResult(List<OperationResult> stepResults) {

        public boolean success() {
            return !stepResults.isEmpty() && stepResults.get(stepResults.size() - 1).success();
        }

        public Payload finalPayload() {
            if (stepResults.isEmpty()) {
                return Payload.empty();
            }
            OperationResult last = stepResults.get(stepResults.size() - 1);
            return last.success() ? last.payload() : Payload.empty();
        }
    }
}
