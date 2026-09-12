export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type JsonValue =
  | string
  | number
  | boolean
  | null
  | JsonValue[]
  | { [key: string]: JsonValue };

export interface HeaderEntry {
  name: string;
  value: string | null;
  secret: boolean;
}

export interface HttpTemplateRequest {
  name: string;
  description: string | null;
  method: HttpMethod;
  urlTemplate: string;
  headerTemplate: HeaderEntry[] | null;
  bodyTemplate: JsonValue | null;
  declaredOutput: JsonValue | null;
}

export interface HttpTemplateResponse {
  id: number;
  name: string;
  description: string | null;
  version: number;
  method: HttpMethod;
  urlTemplate: string;
  headerTemplate: HeaderEntry[];
  bodyTemplate: JsonValue | null;
  declaredOutput: JsonValue | null;
}

export interface ChainStepRequest {
  templateId: number;
  order: number;
}

export interface ChainStepResponse {
  templateId: number;
  order: number;
}

export interface ChainRequest {
  name: string;
  description: string | null;
  steps: ChainStepRequest[];
}

export interface ChainResponse {
  id: number;
  name: string;
  description: string | null;
  steps: ChainStepResponse[];
}

export interface ChainStepResultResponse {
  templateId: number;
  success: boolean;
  payload: JsonValue | null;
  error: string | null;
}

export interface ChainRunResponse {
  chainId: number;
  steps: ChainStepResultResponse[];
  finalPayload: JsonValue;
}

export interface ApiErrorResponse {
  message?: string;
  detail?: string;
  title?: string;
  errors?: Array<{ field?: string; defaultMessage?: string }>;
}
