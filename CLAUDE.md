# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Spring Boot 3.3 / Java 17 stub REST API for retail store operations (in-memory storage, no database).

## Commands

- `mvn test` — JUnit 5 + MockMvc tests. Checkstyle is bound to `validate`, so a style violation fails the build before tests run.
- `mvn test -Dtest=StoreOpsApiTest#createsAndFetchesActivity` — run a single test (all tests currently live in `StoreOpsApiTest`).
- `mvn verify` — tests + SpotBugs (threshold Medium, exclusions in `config/spotbugs-exclude.xml`).
- `mvn spring-boot:run` — start on :8080.
- `mvn checkstyle:check` — style check only (config in `config/checkstyle.xml`: 120-col lines, no star/unused imports, braces required, newline at EOF).

## Architecture

Feature modules under `com.storeops.*`: `activities`, `programmes`, `staff`, `alerts`, `reports`. Each follows Controller → Service → Repository (in-memory). Shared code is in `common.error` and `common.event`.

Cross-module rules (the non-obvious part; keep to them when adding code):
- **No circular module imports.**
- **Cross-module notification goes only through `common.event.EventBus`** (wraps Spring's `ApplicationEventPublisher`). Events implement `DomainEvent`; e.g. `ActivityService.create` publishes `ActivityCreatedEvent`, which `alerts` reacts to.
- **Cross-module reads go through read-only interfaces** such as `ActivityReader` and `StaffReader` (the service implements it). `staff` is exposed to other modules only as `StaffReader`. `reports` should consume these rather than other modules' services/repositories.
- **Throw only `AppError` subclasses** (`NotFoundError`, `ConflictError`, `ValidationError`, `InternalError` in `common.error`) from services/controllers; `GlobalExceptionHandler` maps them to `ErrorResponse`. Checkstyle `IllegalThrows` partially enforces this (plus `IllegalCatch` for `Error`/`Throwable`).

Service constructors are package-private; request/entity types are records (e.g. `Activity`, `CreateActivityRequest`).
