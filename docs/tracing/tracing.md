# apiWeaver — Build Trace

A running log of what's being worked on, kept short on purpose.

**Convention:** append a dated entry per work session — what changed and why, a sentence or two, most recent first. Once this file gets long, prune entries whose content has already been folded into `architecture.md` or `concept.md` (git history keeps the permanent record; this file doesn't need to). This is a working log for whoever — human or Claude Code — picks the project up next, not an audit trail.

---

## 2026-09-12 (test suite)
Filled out the test pyramid for `services/api-core`, split by purpose:
- **Unit tests** (`services/`, `domain/common/`) — `HttpTemplateServiceImplTest`, `ChainServiceImplTest`, `ChainExecutionServiceImplTest`, `HttpTemplateEngineMapperTest`, `JsonConvertersTest` (the JSON `AttributeConverter`s round-tripped directly, no Spring context). All Mockito-based, no Spring context, run in milliseconds.
- **Controller (web-slice) tests** (`web/httptemplate`, `web/chain`) — `@WebMvcTest` with the service layer mocked via `@MockitoBean`; only request mapping, bean validation, and status/error-mapping are exercised. Needed an explicit `@Import(JacksonConfig.class)` since our Jackson-2 `MappingJackson2HttpMessageConverter` isn't Spring Boot's default in this Jackson-3-first version and slice tests don't reliably auto-detect it.
- **Integration tests** (`integration/`) — full `@SpringBootTest` + real H2: `ChainExecutionIntegrationTest` (moved from `web/`, unchanged) and a new `HttpTemplateCrudIntegrationTest` proving the JSON-column converters survive an actual persist/reload, not just the in-memory round trip.
- **Behavior tests** (`behavior/`) — JUnit 5 `@Nested` + `@DisplayName` given/when/then specs (no new framework — Cucumber was considered and declined) for the Engine's core contract (`EngineBehaviorTest`) and the HttpTemplate versioning/secret-masking rules (`HttpTemplateLifecycleBehaviorTest`).

49 tests total, all passing (`./mvnw test`).

Added `src/test/resources/application.yaml` overriding the datasource to an isolated in-memory H2 (`jdbc:h2:mem:apiweaver-test-${random.uuid}`, `ddl-auto: create-drop`) instead of the file-mode DB from main resources — every `@SpringBootTest` context now gets its own throwaway schema, and running the full suite (`./mvnw clean verify`) no longer touches `services/api-core/data/` at all.

## 2026-09-12 (Postman collection)
Added `docs/postman/apiWeaver.postman_collection.json` + `.postman_environment.json`, covering every endpoint (`/http-templates` and `/chains` CRUD, `/chains/{id}/run`), organized into HTTP Templates / Chains / Cleanup folders. The two "Create HTTP Template" requests and "Run Chain" reproduce the GET → POST worked example from architecture.md §7 against the public `jsonplaceholder.typicode.com` API, with test scripts chaining ids through collection variables — runnable end to end with no fixtures needed.

Building it against a real running instance surfaced an actual bug: `HttpOperation` never set `Content-Type` when sending a body, so real APIs (jsonplaceholder included) silently ignored the payload and echoed only a fake id. Fixed in [HttpOperation.java](services/api-core/src/main/java/com/projfiftyk/apicore/engine/http/HttpOperation.java) — defaults to `application/json` when a body is present and no header already sets it, verified against the same public API, and covered by a new [HttpOperationTest](services/api-core/src/test/java/com/projfiftyk/apicore/engine/http/HttpOperationTest.java) (52 tests total now).

## 2026-09-12 (HTTP status now gates success)
Fixed a real correctness bug: `HttpOperation` reported success on any response it could parse as JSON, so a 404/500 with a well-formed JSON error body was reported as a successful step. `execute()` now checks the status code and throws (converted by `AbstractOperation`'s existing catch-all into `OperationResult.failure(...)`) for anything outside 2xx — a 404 no longer masquerades as "operation was fine". Documented in [architecture.md](../architecture/architecture.md) §6 and the decisions log.

Covered by a new [HttpOperationTest](services/api-core/src/test/java/com/projfiftyk/apicore/engine/http/HttpOperationTest.java) status-code suite and a new behavior spec, [HttpOperationStatusBehaviorTest](services/api-core/src/test/java/com/projfiftyk/apicore/behavior/HttpOperationStatusBehaviorTest.java). 59 tests total, all passing.

<!-- Newest entries go here. Example:

## 2026-09-13
Started `backend/` — Engine, Operation, OperationResult per architecture.md. No persistence yet.

-->

## 2026-09-12
Scaffolded the full MVP vertical slice in `services/api-core`: framework-free `engine` package (Payload, Operation/AbstractOperation, Engine, HttpOperation, JMESPath-based ExpressionResolver, OperationFactory — guarded by an ArchUnit test forbidding `org.springframework.*` imports), `HttpTemplate`/`Chain` JPA entities with JSON-column converters, CRUD services/controllers, and `ChainExecutionService` implementing the seam from architecture.md §11 (entity → engine config → Operation → Engine.run → response DTO). H2 file-mode datasource wired per architecture.md §2.

Adopted the `domain/[section]`, `services/[section]`, `transfer/[section]/{request,response}`, `repository/[section]`, `web/[section]` package convention throughout, plus a couple of pragmatic additions: a top-level `config` package (Spring wiring for the engine and Jackson) and `services/common`/`web/common` for the shared `NotFoundException` / `GlobalExceptionHandler`.

Notable snag: Spring Boot 4.1.1 (already in the scaffold's `pom.xml`) defaults to Jackson 3 (`tools.jackson.*`), but `io.burt:jmespath-jackson` (used for `{{expr}}` resolution) only supports Jackson 2 (`com.fasterxml.jackson.*`). Standardized the whole app on Jackson 2 instead of running two Jackson majors side by side — `config/JacksonConfig` defines the `ObjectMapper` bean and registers `MappingJackson2HttpMessageConverter` as the primary Spring MVC converter, and `spring-boot-starter-jackson` was deliberately left out of `pom.xml`.

Verified end-to-end: `./mvnw test` (5 tests: Engine unit tests, the ArchUnit rule, a full GET→POST chain run against a real localhost server via MockMvc) and a manual `spring-boot:run` + curl pass against `/http-templates`, `/chains`, and `/chains/{id}/run`.

Deferred/known limitations carried over from architecture.md, not revisited here: `{{expr}}` always resolves to a string; header `secret` values are flagged but not encrypted at rest; `OperationResult` has no HTTP status/metadata field, so the run endpoint currently reports `success` based only on network/parsing errors, not HTTP status codes.