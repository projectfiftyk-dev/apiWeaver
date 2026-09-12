import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { HttpTemplateResponse } from "../api/types";
import MethodBadge from "../components/MethodBadge";

export default function TemplatesPage() {
  const [templates, setTemplates] = useState<HttpTemplateResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load() {
    setError(null);
    api.templates
      .list()
      .then(setTemplates)
      .catch((e: ApiError) => setError(e.message));
  }

  useEffect(load, []);

  async function handleDelete(id: number, name: string) {
    if (!confirm(`Delete template "${name}"? This cannot be undone.`)) return;
    try {
      await api.templates.delete(id);
      load();
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Templates</h1>
          <p className="page-subtitle">Reusable, parameterized HTTP request definitions.</p>
        </div>
        <Link to="/templates/new" className="btn btn-primary">
          + New template
        </Link>
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {templates === null && !error && <p className="loading">Loading templates…</p>}

      {templates && templates.length === 0 && (
        <div className="empty-state">
          No templates yet. Create your first one to get started.
        </div>
      )}

      {templates && templates.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Method</th>
              <th>URL</th>
              <th>Version</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {templates.map((t) => (
              <tr key={t.id}>
                <td>
                  <Link to={`/templates/${t.id}`} className="mono">
                    {t.name}
                  </Link>
                </td>
                <td>
                  <MethodBadge method={t.method} />
                </td>
                <td className="mono text-muted">{t.urlTemplate}</td>
                <td className="text-muted">v{t.version}</td>
                <td className="table-actions">
                  <Link to={`/templates/${t.id}`} className="btn btn-sm">
                    Edit
                  </Link>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(t.id, t.name)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
