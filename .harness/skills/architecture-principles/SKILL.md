---
name: architecture-principles
description: The five StoreOps architecture rules (G1-G5), each with a violation example and the correct form. Read before writing or reviewing any code.
---

# Architecture principles

Each rule is enforced by a test in `src/test/java/com/storeops/architecture/ArchitectureRulesTest.java`. Never edit that file to make code pass.

## G1 Module boundaries
A module touches another module only through that module's `*Reader` interface (plus `common.*` and plain data records like `Staff`).
- Violation: `ReportService` takes `ActivityService` or `ActivityRepository` in its constructor.
- Correct: take `ActivityReader`. Need a new read? Add a method to the Reader interface in the owning module and implement it in that module's Service (`ActivityService implements ActivityReader`).
- Also: no cycles between modules. If `activities` needs `staff` data, `staff` must not need `activities`.

## G2 AppError contract
Application code constructs only `AppError` subclasses: `NotFoundError(resource, id)` 404, `ConflictError(msg)` 409, `ValidationError(msg)` 400, `ForbiddenError(msg)` 403, `InternalError(msg)` 500. `GlobalExceptionHandler` turns them into `ErrorResponse`.
- Violation: `throw new IllegalStateException("...")`, `new RuntimeException`, `new Error`, returning `ResponseEntity.status(404)` from a controller.
- Pitfall: `com.storeops.common.error.InternalError` shares its simple name with `java.lang.InternalError`. Import the StoreOps one explicitly.
- Checkstyle `IllegalThrows` does NOT catch `throw new X()`; only the ArchUnit gate does.

## G3 Event bus for cross-module triggers
If module A's state change must make module B react, A calls `EventBus.publish(new SomeEvent(...))` and B has an `@EventListener`. A never calls B.
- Violation: `ActivityService` calls `AlertService.create(...)` directly; or injects `ApplicationEventPublisher`.
- Correct: add a record implementing `DomainEvent` in `common.event`; publish via `EventBus`; the listener lives in the reacting module (see `AlertService.onActivityCreated`).
- Every state-changing Service method publishes an event after the change succeeds, never before the permission check. Existing examples: `ActivityService.create` / `update` / `delete`. No static gate can see this, so grading-criteria A-G6 requires an event test (`@RecordApplicationEvents`) for each state change.

## G4 Layer separation
Controller -> Service -> Repository. Controllers hold routing and `@Valid` only. Business rules live in the Service. Repositories are package-private and only store and fetch.
- Violation: a controller calling `repository.findById`; a rule such as the delete permission check placed in the controller.

## G5 Reports are read-only
`reports` computes from `ActivityReader` and `StaffReader` only. It never changes another module's state. Its own `ReportRepository` cache is the one thing it may write.
- Violation: a report endpoint that updates an activity or acknowledges an alert.
- A new report that needs data from `alerts` or `programmes` must add a Reader interface in that module first.
