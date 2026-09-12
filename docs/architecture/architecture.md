# apiWeaver — Architecture

Status: MVP design, pre-implementation. First build target: a `Chain` of two `HttpTemplate`s (GET → POST) executed end to end, with tests.

See the root `README.md` for what apiWeaver is and why. This document is the technical design: the core model, the patterns behind it, and the concrete backend surface to implement first.

---

## 1. Repository layout

```
apiweaver/
├── README.md        product concept — see docs/architecture.md for the technical design
├── ui/              React + TypeScript frontend
├── services/        every backend service lives here
│   └── core-api/    Spring Boot — Engine, Operation, Templates, REST API, persistence (the only service today)
└── docs/
    ├── architecture.md   this file — the technical design
    └── tracing.md        running build log (see convention below)
```

`services/` is the umbrella for **every** backend service, not just this one. `core-api` — the Spring Boot app this document specs out, holding the Engine/Operation/Template logic and its REST API — is the first and only entry today, but the folder is structured to hold siblings as the system grows: an API gateway in front of everything, a UI-specific BFF, and the eventual AI-planner service (Python/FastAPI). None of those exist yet; this is just making sure `core-api` doesn't end up sitting at the repository root as if it were the only backend service apiWeaver will ever have.

**`docs/tracing.md` convention:** append short, dated entries as work happens (what changed, why) — most recent first. Once it gets long, prune entries whose content has already been folded into `architecture.md` or the root `README.md`. It's meant to stay a short, current log of in-flight work, not a permanent history — git already keeps that.

## 2. Persistence

**H2, embedded, file mode** — e.g. `jdbc:h2:file:./data/apiweaver` — not Postgres, for now.

Why H2 over another file-based option (SQLite, the other common choice): H2 has a real, complete Hibernate dialect and native Spring Boot auto-configuration, so JPA behaves exactly as it would against Postgres — no partial/community dialect quirks to work around. It ships a web console for inspecting the file during development, needs no server process or docker-compose for local dev, and migrating to Postgres later is close to just swapping the connection string and driver, since JPA/Hibernate already abstracts the SQL dialect.

## 3. Authorization

Postponed entirely. No auth on any endpoint for the MVP.

## 4. MVP scope

**In scope:** the Engine (sequential execution of a list of Operations); one Operation type, `HttpOperation`; `HttpTemplate` with `{{expression}}` resolution on every field; `Chain` (an ordered, named list of Templates) and its run endpoint; Spring Boot (core + API) and React/TS (UI) only.

**Explicitly deferred:** `TransformationOperation` / `TransformationTemplate`; collections/environments (Postman-style); a visual node-graph chain builder; branching, fan-out, retry/continue-on-error; non-JSON formats and file handling; FastAPI and any LLM-assisted template authoring; authorization.

## 5. The Engine

The Engine knows exactly one thing: given an ordered list of Operations and a seed input, run them in sequence, feeding each result forward, and stop at the first failure. Nothing about Templates, HTTP, or persistence belongs here — it depends only on the `Operation` interface (Dependency Inversion), never on how a concrete Operation was built or configured.

```
function Engine.run(operations: List<Operation>, seedInput: Payload) -> OperationResult:
    currentInput = seedInput
    lastResult   = null

    for operation in operations:
        lastResult = operation.run(currentInput)

        if lastResult.success == false:
            return lastResult          // stop here — this is the failure to report

        currentInput = lastResult.payload   // this step's payload becomes the next step's input

    return lastResult   // whatever the last operation produced
```

**Reserved vocabulary** — used consistently everywhere below:

- **Payload** — the plain JSON object flowing through `run()` at runtime. Nothing else is ever called this.
- **Template** — the persisted, named, editable config that a Factory turns into a live Operation. Nothing else is ever called this.

## 6. The Operation contract

Every Operation — regardless of type — honors one contract: `run(payload) → OperationResult`. The Engine only ever calls this. What happens inside is a fixed three-step skeleton (Template Method):

```
Operation.run(payload):
    bound   = bindInput(payload)     // adapt the incoming Payload using this Operation's Template config
    raw     = execute(bound)         // the type-specific work (fire a request, or resolve a mapping)
    output  = mapOutput(raw)         // normalize whatever came out into a plain Payload
    return OperationResult(success: true, payload: output)
    // any failure along the way -> OperationResult(success: false, error: message)
```

### OperationResult

| Field | Type | Notes |
|---|---|---|
| `success` | bool | Discriminates the other two fields — a Result type, not an exception. |
| `payload` | Payload | Present only when `success == true`. |
| `error` | string | Present only when `success == false`. A structured error object is a reasonable later upgrade, not needed for MVP. |

Deferred on purpose: a `metadata` slot for things like HTTP status codes and response headers. Adding it later is purely additive.

For `HttpOperation` specifically, `success` is determined by the HTTP response status: any 2xx is success, anything else (4xx, 5xx) is `success == false` with `error` describing the status and body — a well-formed JSON error body from a 404 must never be reported as a successful step just because it parsed.

## 7. Templates

A Template is fixed at authoring time, with placeholders for whatever can only be known once a real Payload arrives. Every value inside one is either a literal, or a string containing one or more `{{expression}}` placeholders resolved against the current Payload via **JMESPath**.

For MVP, `{{expression}}` always resolves to a string, even when the source value is a different JSON type (e.g. a number) — a known, consciously accepted limitation (see Decisions log).

### HttpTemplate

| Field | Required | Shape | Resolved by |
|---|---|---|---|
| `method` | required | enum: GET / POST / PUT / PATCH / DELETE | fixed — never templated (a plain field, not its own sub-template) |
| `urlTemplate` | required | single string | `{{expr}}` resolved once against the whole string |
| `headerTemplate` | optional | flat map of string → string | each value: literal, `{{expr}}`, or `secret`-flagged (masked, encrypted at rest, never payload-derived) |
| `bodyTemplate` | optional | nested JSON tree — string / number / float / list / object | `{{expr}}` resolved at any leaf |

`UrlTemplate`, `HeaderTemplate`, and `BodyTemplate` are **not** separate entities or independently reusable — they're nested fields owned by exactly one `HttpTemplate`, with no ID or REST resource of their own.

> **Naming note:** `bodyTemplate` was originally called `PayloadTemplate`. Renamed because the word "Payload" is reserved for the runtime object flowing through `run()` — a name containing it repeatedly caused the same mix-up (a Transformation producing "a Payload," not "a PayloadTemplate").

### TransformationTemplate — *deferred for MVP*

Takes a Payload, produces a new Payload. A set of target-field → expression mapping rules, resolved with the same `{{expr}}` / JMESPath mechanism `HttpTemplate` uses internally — just applied to build a whole new object instead of a piece of a request.

| | Payload |
|---|---|
| Input | `{ "name": "John", "age": 16 }` |
| Mapping rule | `{ full_data: join(' ', [name, to_string(age)]), createdAt: 'someConstant' }` |
| Output | `{ "full_data": "John 16", "createdAt": "someConstant" }` |

**Why this can wait:** every value in `HttpTemplate` already supports `{{expression}}`, so simple selection and pass-through between two Http calls needs no Transformation step at all — an unreferenced field is just never in the next request (see the worked GET→POST example below). Bring this back the moment one of three things shows up: no next Http step to bind into (a chain's final output), a derivation complex enough to deserve its own visible, debuggable step, or reshaping logic reused in more than one place. Adding it back changes nothing about the Engine or the `Operation` contract — it's a second Strategy, which is exactly what that interface was built to accept.

**Worked example — why Transformation is skippable here:**

```
Request 1 — GET http.example.com/product/someproductid
→ { "id": "someId", "name": "Example", "age": 15 }

Request 2 — POST http.example.com/product/
Body: { "name": "{{name}}", "age": {{age}} }
```

`id` is never referenced, so it's simply absent from request 2 — no removal step needed. `age` is a whole-value placeholder (`{{age}}`, not embedded in a longer string), so it should resolve to the actual JSON type from the Payload rather than a stringified version — see the type-resolution note above; for MVP this still lands as a string, which is why it's flagged as a known limitation rather than a forgotten one.

## 8. Factory: Template → Operation

A Template is data. Something has to turn that data into a live, runnable Operation before the Engine's loop can call `.run()` on it — that's the Factory's entire job: given a Template's type and config, construct the matching Operation instance, already configured.

This is the seam between the CRUD side of apiWeaver (saving and editing Templates) and the execution side (the Engine running Operations). CRUD never touches behavior; the Factory is the only thing that turns saved config into something runnable.

## 9. Pattern glossary

Every entry below showed up by necessity while designing apiWeaver, not by decree. **GoF** — "Gang of Four" — is shorthand for the four authors (Gamma, Helm, Johnson, Vlissides) of the 1994 catalog that named 23 recurring object-oriented design patterns; not every entry here is from that book.

| Pattern | Category | What it means | Where it shows up |
|---|---|---|---|
| Strategy | Behavioral, GoF | Interchangeable behaviors behind one shared interface — the caller never needs to know which concrete one it's holding. | The `Operation` interface — `HttpOperation` is its only concrete Strategy for now, but the interface costs nothing to extend when `TransformationOperation` comes back. |
| Template Method | Behavioral, GoF | A fixed sequence of steps, where each concrete type only fills in the steps that vary. | `bindInput → execute → mapOutput` inside every Operation's `run()`. |
| Factory Method | Creational, GoF | A dedicated piece of code that builds the right concrete object from some input, so nothing else needs scattered type checks. | Turning a stored Template into a live Operation instance. |
| Dependency Inversion | SOLID principle | A component depends on an abstraction, never on how concrete instances of it get built or sourced. | The Engine depends only on the `Operation` interface — never on Templates, Factories, or persistence. |
| Result type (a.k.a. Either) | — | An explicit success-or-failure return value in place of throwing exceptions. | `OperationResult` — checked with a plain `if`, never a `try/catch`. |
| Pipeline | Architectural pattern, not GoF | A sequence of stages, each one always processing and passing its output to the next. | The Engine's whole loop. |
| Chain of Responsibility | Behavioral, GoF — for contrast | Each handler can choose to act, pass through untouched, or stop the chain — unlike Pipeline, where every stage always acts. | Explicitly *not* what the Engine does — useful to know the difference. |
| Hexagonal Architecture (Ports & Adapters) | — | Core domain logic stays framework-free at the center; a REST controller, a desktop UI, a CLI are all interchangeable adapters around it. | Why `Engine` / `Operation` / `Template` code must never import Spring. |
| Observer | Behavioral, GoF — deferred | Outside listeners subscribe to "something happened" notifications without the subject knowing who's listening. | Future real-time execution updates — not built yet. |
| Message Translator | Enterprise Integration Patterns | A component whose entire job is converting one message's shape into another. | What `TransformationTemplate` literally is, in integration-pattern vocabulary. |

## 10. Project structure & packaging

Engine, Operation, OperationResult, and every Template type live as a **plain-Java package inside `services/core-api/`** — not a separate module, for now. What actually protects the future is a rule, not a folder boundary:

> This package may depend on plain Java and framework-agnostic libraries only — never on `org.springframework.*`.

Extraction into its own module later, or reuse from a desktop app, then stays a mechanical cut-and-paste rather than an untangling job. Worth enforcing later with an **ArchUnit** test that fails the build on a stray Spring import.

Because of this, a Template needs to stay plain, portable JSON — exportable and runnable by the same core library outside the server entirely, which also answers anyone unwilling to route sensitive request payloads through a third-party backend.

## 11. Backend surface — entities & endpoints

The first implementation milestone: two persisted entities, two CRUD controllers, and one run endpoint — enough to define a GET and a POST template, chain them, and execute them for real.

### `HttpTemplate` (entity)

One table. `UrlTemplate`, `HeaderTemplate`, and `BodyTemplate` are nested fields on this one, not separate entities.

| Field | Shape |
|---|---|
| `id`, `name`, `description`, `version` | standard record fields |
| `method` | enum — plain field, not its own Template |
| `urlTemplate` | string, may contain `{{expr}}` |
| `headerTemplate` | list of `{name, value, secret}` |
| `bodyTemplate` | nested JSON, stored as a JSON column |
| `declaredOutput` | optional example JSON — feeds the "available fields" UI later |

### `Chain` (entity)

A named, ordered list of `HttpTemplate` references — what "run everything" actually runs.

| Field | Shape |
|---|---|
| `id`, `name`, `description` | standard record fields |
| `steps` | ordered list of `{templateId, order}` |

### Controllers

| Resource | Endpoints | Responsibility |
|---|---|---|
| `/http-templates` | `POST` · `GET` · `GET /{id}` · `PUT /{id}` · `DELETE /{id}` | CRUD on `HttpTemplate`, sub-templates included in the same payload |
| `/chains` | `POST` · `GET` · `GET /{id}` · `PUT /{id}` · `DELETE /{id}` | CRUD on the ordered list of template references |
| `/chains/{id}/run` | `POST` | builds each step's Operation via the Factory, runs the Engine, returns the trace |

Suggested `run` response — the full per-step trace, not just the final payload:

```json
{
  "chainId": "...",
  "steps": [
    { "templateId": "...", "success": true, "payload": { }, "error": null }
  ],
  "finalPayload": { }
}
```

The Engine already produces this internally; returning it costs nothing and is exactly what a first smoke test needs to see which step failed and why. Persisting this as queryable run-history (`Execution`) is a reasonable fast-follow, not needed for this first milestone.

**Where Spring is and isn't allowed:** the JPA `@Entity` classes backing these tables are *not* the same classes as the core `HttpTemplate` / `Operation` model — they're a separate, Spring-flavored persistence representation. A thin Spring service loads entities via the repository, maps them into plain-Java Template config, hands that to the Factory and Engine, and maps the plain-Java `OperationResult`s back into a response DTO. That mapping step is the actual seam — the one place Spring and the framework-free core touch.

## 12. Decisions log

| Decision | Status | Note |
|---|---|---|
| `OperationResult` value field named `payload` | Decided | went `result` → `payload`, matching the reserved Payload type name |
| `PayloadTemplate` renamed `BodyTemplate` | Decided | removes collision with the reserved word "Payload" |
| `TransformationOperation` / `TransformationTemplate` in the MVP | Deferred | Http's own `{{}}` field bindings cover simple selection; design stays documented, ready to re-add as a second Strategy |
| HTTP method as its own sub-template | Decided | plain enum field on `HttpTemplate` instead — no case yet needs a computed method |
| Url/Headers/Body sub-templates independently reusable | Decided | embedded, owned by exactly one `HttpTemplate` — one entity, one CRUD controller, no separate REST resource |
| "Operations" CRUD | Decided | revives **Chain** — a named, ordered list of `HttpTemplate` references; this is what the run endpoint executes |
| `{{variable}}` type resolution | Deferred | always resolves as a string for now — a known limitation (e.g. numeric fields get stringified), revisit when it actually bites |
| Engine keeps an explicit seed input | Decided | not hardcoded `null` internally — a real parameter, which happens to start as `null`/empty for the first tests |
| Query-param secrets | Open | header-level secrets are designed; query-level isn't yet |
| `OperationResult.metadata` (HTTP status / headers) | Deferred | conscious punt — purely additive when it's added |
| `HttpOperation` success gated on HTTP status | Decided | 2xx only; a parseable 4xx/5xx body is still a failure, not success — fixed after the initial implementation reported any parseable response as successful regardless of status |
| FastAPI / LLM planner in the MVP | Decided | deferred entirely — MVP is Spring Boot + React only |
| Zero-Spring-dependency rule on the core package | Decided | enforce with ArchUnit once the project exists |
| Chain builder UI: reorderable list vs. visual graph | Decided | list for MVP; graph builder is a fast-follow |
| Collections / environments (Postman-style) | Deferred | explicitly out of MVP scope |
| Repository layout: `services/` holds every backend service, `ui/`, `docs/`; concept doc lives as root `README.md` | Decided | `core-api` (this Spring Boot app) is the first of several planned services — a future API gateway, UI BFF, and AI-planner service will be siblings under `services/` |
| Persistence: H2, embedded, file mode | Decided | not Postgres, for now — see rationale above |
| Authorization | Deferred | no auth on any endpoint for the MVP |