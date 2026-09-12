import type { HttpMethod } from "../api/types";

export default function MethodBadge({ method }: { method: HttpMethod }) {
  return <span className={`badge badge-${method.toLowerCase()}`}>{method}</span>;
}
