import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { HeaderEntry, HttpMethod, JsonValue } from "../api/types";
import HeaderListEditor from "../components/HeaderListEditor";
import JsonField from "../components/JsonField";

const METHODS: HttpMethod[] = ["GET", "POST", "PUT", "PATCH", "DELETE"];

function parseOptionalJson(text: string): { value: JsonValue | null; error: string | null } {
  const trimmed = text.trim();
  if (trimmed === "") return { value: null, error: null };
  try {
    return { value: JSON.parse(trimmed), error: null };
  } catch {
    return { value: null, error: "Not valid JSON." };
  }
}

export default function TemplateEditorPage() {
  const { id } = useParams();
  const isNew = id === undefined;
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [method, setMethod] = useState<HttpMethod>("GET");
  const [urlTemplate, setUrlTemplate] = useState("");
  const [headers, setHeaders] = useState<HeaderEntry[]>([]);
  const [bodyText, setBodyText] = useState("");
  const [declaredOutputText, setDeclaredOutputText] = useState("");
  const [bodyError, setBodyError] = useState<string | null>(null);
  const [declaredOutputError, setDeclaredOutputError] = useState<string | null>(null);
  const [loading, setLoading] = useState(!isNew);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isNew) return;
    api.templates
      .get(Number(id))
      .then((t) => {
        setName(t.name);
        setDescription(t.description ?? "");
        setMethod(t.method);
        setUrlTemplate(t.urlTemplate);
        setHeaders(t.headerTemplate);
        setBodyText(t.bodyTemplate === null ? "" : JSON.stringify(t.bodyTemplate, null, 2));
        setDeclaredOutputText(
          t.declaredOutput === null ? "" : JSON.stringify(t.declaredOutput, null, 2)
        );
      })
      .catch((e: ApiError) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id, isNew]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const body = parseOptionalJson(bodyText);
    const declaredOutput = parseOptionalJson(declaredOutputText);
    setBodyError(body.error);
    setDeclaredOutputError(declaredOutput.error);
    if (body.error || declaredOutput.error) return;

    setSaving(true);
    try {
      const payload = {
        name,
        description: description.trim() === "" ? null : description,
        method,
        urlTemplate,
        headerTemplate: headers,
        bodyTemplate: body.value,
        declaredOutput: declaredOutput.value,
      };
      if (isNew) {
        await api.templates.create(payload);
      } else {
        await api.templates.update(Number(id), payload);
      }
      navigate("/templates");
    } catch (err) {
      setError((err as ApiError).message);
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <p className="loading">Loading template…</p>;

  return (
    <div>
      <a className="back-link" href="#" onClick={(e) => { e.preventDefault(); navigate("/templates"); }}>
        ← Templates
      </a>
      <div className="page-header">
        <h1>{isNew ? "New template" : `Edit template`}</h1>
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      <form className="form" onSubmit={handleSubmit}>
        <div className="form-row">
          <div className="form-group">
            <label>Name</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Get user by id"
            />
          </div>
          <div className="form-group">
            <label>Method</label>
            <select value={method} onChange={(e) => setMethod(e.target.value as HttpMethod)}>
              {METHODS.map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
          </div>
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
          <label>URL template</label>
          <span className="form-hint">
            May contain <code>{"{{expression}}"}</code> placeholders resolved via JMESPath.
          </span>
          <input
            type="text"
            required
            className="mono"
            value={urlTemplate}
            onChange={(e) => setUrlTemplate(e.target.value)}
            placeholder="https://api.example.com/users/{{userId}}"
          />
        </div>

        <HeaderListEditor headers={headers} onChange={setHeaders} />

        <JsonField
          label="Body template"
          hint="Optional JSON body. Leaves may contain {{expression}} placeholders."
          value={bodyText}
          error={bodyError}
          onChange={setBodyText}
          placeholder='{"userId": "{{input.id}}"}'
        />

        <JsonField
          label="Declared output"
          hint="Optional example of the expected response shape, for documentation only."
          value={declaredOutputText}
          error={declaredOutputError}
          onChange={setDeclaredOutputText}
          rows={4}
        />

        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? "Saving…" : isNew ? "Create template" : "Save changes"}
          </button>
          <button type="button" className="btn btn-ghost" onClick={() => navigate("/templates")}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
