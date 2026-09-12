# apiWeaver UI

Minimal React + TypeScript frontend for the `api-core` backend: manage `HttpTemplate`s, compose them into `Chain`s, and run chains to see step-by-step results.

## Setup

```bash
npm install
npm run dev
```

The dev server runs at `http://localhost:5173` and expects the backend at `http://localhost:8080` (override with a `.env` file setting `VITE_API_BASE_URL`). Start the backend first:

```bash
cd ../services/api-core
./mvnw spring-boot:run
```

The backend's `CorsConfig` allows requests from `http://localhost:5173` and `4173` (the Vite preview port) — no other origins are permitted.

## Structure

- `src/api` — typed fetch client mirroring the backend's DTOs (`types.ts`) and endpoints (`client.ts`).
- `src/components` — shared UI pieces (method badges, JSON fields, header/step editors, run results).
- `src/pages` — one page per screen: template list/editor, chain list/editor.
