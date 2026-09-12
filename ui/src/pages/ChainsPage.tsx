import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { ChainResponse, ChainRunResponse } from "../api/types";
import RunResultPanel from "../components/RunResultPanel";

export default function ChainsPage() {
  const [chains, setChains] = useState<ChainResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [runningId, setRunningId] = useState<number | null>(null);
  const [runResult, setRunResult] = useState<{ chainId: number; result: ChainRunResponse } | null>(
    null
  );

  function load() {
    setError(null);
    api.chains
      .list()
      .then(setChains)
      .catch((e: ApiError) => setError(e.message));
  }

  useEffect(load, []);

  async function handleDelete(id: number, name: string) {
    if (!confirm(`Delete chain "${name}"? This cannot be undone.`)) return;
    try {
      await api.chains.delete(id);
      if (runResult?.chainId === id) setRunResult(null);
      load();
    } catch (e) {
      setError((e as ApiError).message);
    }
  }

  async function handleRun(id: number) {
    setError(null);
    setRunningId(id);
    setRunResult(null);
    try {
      const result = await api.chains.run(id);
      setRunResult({ chainId: id, result });
    } catch (e) {
      setError((e as ApiError).message);
    } finally {
      setRunningId(null);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Chains</h1>
          <p className="page-subtitle">Ordered sequences of templates executed end to end.</p>
        </div>
        <Link to="/chains/new" className="btn btn-primary">
          + New chain
        </Link>
      </div>

      {error && <div className="banner banner-error">{error}</div>}

      {chains === null && !error && <p className="loading">Loading chains…</p>}

      {chains && chains.length === 0 && (
        <div className="empty-state">No chains yet. Create one from your templates.</div>
      )}

      {chains && chains.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Steps</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {chains.map((c) => (
              <tr key={c.id}>
                <td>
                  <Link to={`/chains/${c.id}`} className="mono">
                    {c.name}
                  </Link>
                  {c.description && <div className="text-muted">{c.description}</div>}
                </td>
                <td className="text-muted">{c.steps.length}</td>
                <td className="table-actions">
                  <button
                    className="btn btn-sm btn-primary"
                    onClick={() => handleRun(c.id)}
                    disabled={runningId === c.id}
                  >
                    {runningId === c.id ? "Running…" : "Run"}
                  </button>
                  <Link to={`/chains/${c.id}`} className="btn btn-sm">
                    Edit
                  </Link>
                  <button className="btn btn-sm btn-danger" onClick={() => handleDelete(c.id, c.name)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {runResult && <RunResultPanel result={runResult.result} />}
    </div>
  );
}
