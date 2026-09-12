# Postman collection

- `apiWeaver.postman_collection.json` — every `core-api` endpoint (`/http-templates` and `/chains` CRUD, plus `/chains/{id}/run`), organized into **HTTP Templates**, **Chains**, and **Cleanup** folders.
- `apiWeaver.postman_environment.json` — a `baseUrl` variable (defaults to `http://localhost:8080`).

## How it's wired together

The two "Create HTTP Template" requests and "Run Chain" reproduce the GET → POST worked example from `docs/architecture/architecture.md` §7, against the public `jsonplaceholder.typicode.com` test API — no other setup needed beyond `core-api` running locally.

Each create/run request has a test script that captures the id it returns into a collection variable (`getTemplateId`, `postTemplateId`, `chainId`), which later requests read from the URL or body. Run the folders top to bottom, or run the whole collection with Postman's Collection Runner.

A few requests carry a saved example response (visible in Postman's "Examples" dropdown) for cases that only make sense pre-recorded: the `/chains/{id}/run` trace, and the 404/400 error shapes from `GlobalExceptionHandler` and bean validation.

## Import

1. Postman → Import → select both `.json` files here.
2. Pick the "apiWeaver — local" environment in the top-right environment selector.
3. Start `core-api` (`./mvnw spring-boot:run` from `services/api-core`), then run the collection.
