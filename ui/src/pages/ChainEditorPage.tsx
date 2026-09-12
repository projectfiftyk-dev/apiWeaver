import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { ChainStepRequest, HttpTemplateResponse } from "../api/types";

export default function ChainEditorPage() {
  const { id } = useParams();
  const isNew = id === undefined;
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [steps, setSteps] = useState<ChainStepRequest[]>([]);
  const [templates, setTemplates] = useState<HttpTemplateResponse[] | null>(null);
  const [loading, setLoading] = useState(!isNew);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.templates
      .list()
      .then(setTemplates)
      .catch((e: ApiError) => setError(e.message));
  }, []);

  useEffect(() => {
    if (isNew) return;
    api.chains
      .get(Number(id))
      .then((c) => {
        setName(c.name);
        setDescription(c.description ?? "");
        setSteps(c.steps.map((s) => ({ templateId: s.templateId, order: s.order })));
      })
      .catch((e: ApiError) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id, isNew]);

  function updateStep(index: number, patch: Partial<ChainStepRequest>) {
    const next = steps.slice();
    next[index] = { ...next[index], ...patch };
    setSteps(next);
  }

  function removeStep(index: number) {
    setSteps(steps.filter((_, i) => i !== index));
  }

  function addStep() {
    const nextOrder = steps.length === 0 ? 1 : Math.max(...steps.map((s) => s.order)) + 1;
    const firstTemplateId = templates && templates.length > 0 ? templates[0].id : 0;
    setSteps([...steps, { templateId: firstTemplateId, order: nextOrder }]);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (steps.length === 0) {
      setError("A chain needs at least one step.");
      return;
    }

    setSaving(true);
    try {
      const payload = {
        name,
        description: description.trim() === "" ? null : description,
        steps: steps.map((s) => ({ ...s, templateId: Number(s.templateId) })),
      };
      if (isNew) {
        await api.chains.create(payload);
      } else {
        await api.chains.update(Number(id), payload);
      }
      navigate("/chains");
    } catch (err) {
      setError((err as ApiError).message);
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <p className="loading">Loading chain…</p>;

  return (
    <div>
      <a
        className="back-link"
        href="#"
        onClick={(e) => {
          e.preventDefault();
          navigate("/chains");
        }}
      >
        ← Chains
      </a>
      <div className="page-header">
        <h1>{isNew ? "New chain" : "Edit chain"}</h1>
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {templates && templates.length === 0 && (
        <div className="banner banner-error">
          No templates exist yet — create at least one template before building a chain.
        </div>
      )}

      <form className="form" onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Name</label>
          <input type="text" required value={name} onChange={(e) => setName(e.target.value)} />
        </div>

        <div className="form-group">
          <label>Description</label>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional"
          />
        </div>

        <div className="form-group">
          <label>Steps</label>
          <span className="form-hint">Executed in ascending order.</span>
          {steps.map((step, index) => (
            <div className="step-row" key={index}>
              <input
                type="number"
                value={step.order}
                onChange={(e) => updateStep(index, { order: Number(e.target.value) })}
              />
              <select
                value={step.templateId}
                onChange={(e) => updateStep(index, { templateId: Number(e.target.value) })}
              >
                {(templates ?? []).map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name} ({t.method})
                  </option>
                ))}
              </select>
              <button
                type="button"
                className="icon-btn"
                onClick={() => removeStep(index)}
                title="Remove step"
              >
                ✕
              </button>
            </div>
          ))}
          <div>
            <button
              type="button"
              className="btn btn-sm"
              onClick={addStep}
              disabled={!templates || templates.length === 0}
            >
              + Add step
            </button>
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? "Saving…" : isNew ? "Create chain" : "Save changes"}
          </button>
          <button type="button" className="btn btn-ghost" onClick={() => navigate("/chains")}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
