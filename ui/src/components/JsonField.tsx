interface JsonFieldProps {
  label: string;
  hint?: string;
  value: string;
  error?: string | null;
  onChange: (value: string) => void;
  rows?: number;
  placeholder?: string;
}

export default function JsonField({
  label,
  hint,
  value,
  error,
  onChange,
  rows = 5,
  placeholder,
}: JsonFieldProps) {
  return (
    <div className="form-group">
      <label>{label}</label>
      {hint && <span className="form-hint">{hint}</span>}
      <textarea
        className={error ? "has-error" : ""}
        value={value}
        rows={rows}
        placeholder={placeholder}
        spellCheck={false}
        onChange={(e) => onChange(e.target.value)}
      />
      {error && <span className="error-text">{error}</span>}
    </div>
  );
}
