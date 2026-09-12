import type { HeaderEntry } from "../api/types";

interface HeaderListEditorProps {
  headers: HeaderEntry[];
  onChange: (headers: HeaderEntry[]) => void;
}

export default function HeaderListEditor({ headers, onChange }: HeaderListEditorProps) {
  function update(index: number, patch: Partial<HeaderEntry>) {
    const next = headers.slice();
    next[index] = { ...next[index], ...patch };
    onChange(next);
  }

  function remove(index: number) {
    onChange(headers.filter((_, i) => i !== index));
  }

  function add() {
    onChange([...headers, { name: "", value: "", secret: false }]);
  }

  return (
    <div className="form-group">
      <label>Headers</label>
      <span className="form-hint">
        Values may contain <code>{"{{expression}}"}</code> placeholders. Secret values are
        masked once saved — re-enter to change.
      </span>
      {headers.map((header, index) => (
        <div className="header-row" key={index}>
          <input
            type="text"
            placeholder="Header name"
            value={header.name}
            onChange={(e) => update(index, { name: e.target.value })}
          />
          <input
            type="text"
            placeholder={header.secret && header.value === null ? "•••• (unchanged)" : "Value"}
            value={header.value ?? ""}
            onChange={(e) => update(index, { value: e.target.value })}
          />
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={header.secret}
              onChange={(e) => update(index, { secret: e.target.checked })}
            />
            secret
          </label>
          <button type="button" className="icon-btn" onClick={() => remove(index)} title="Remove header">
            ✕
          </button>
        </div>
      ))}
      <div>
        <button type="button" className="btn btn-sm" onClick={add}>
          + Add header
        </button>
      </div>
    </div>
  );
}
