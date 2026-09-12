import type { ChainRunResponse } from "../api/types";

export default function RunResultPanel({ result }: { result: ChainRunResponse }) {
  const lastStep = result.steps[result.steps.length - 1];
  const overallSuccess = result.steps.length > 0 && lastStep.success;

  return (
    <div className="run-result">
      <div className={`banner ${overallSuccess ? "banner-success" : "banner-error"}`}>
        {overallSuccess
          ? `Chain completed successfully — ${result.steps.length} step(s) ran.`
          : `Chain stopped after ${result.steps.length} step(s) — the failing step's error is shown below.`}
      </div>

      {result.steps.map((step, index) => (
        <div
          key={index}
          className={`run-step ${step.success ? "run-step-success" : "run-step-failure"}`}
        >
          <div className="run-step-head">
            <span>
              Step {index + 1} · Template #{step.templateId}
            </span>
            <span className={step.success ? "text-muted" : "error-text"}>
              {step.success ? "success" : "failed"}
            </span>
          </div>
          {step.success ? (
            <pre className="json-block">{JSON.stringify(step.payload, null, 2)}</pre>
          ) : (
            <pre className="json-block">{step.error}</pre>
          )}
        </div>
      ))}

      <div>
        <p className="section-title">Final payload</p>
        <pre className="json-block">{JSON.stringify(result.finalPayload, null, 2)}</pre>
      </div>
    </div>
  );
}
