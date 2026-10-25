---
name: how-to-test
description: How StoreOps tests must assert business rules, not just HTTP status. Read before writing tests.
---

# How to test

Tests use JUnit 5 + `@SpringBootTest` + `@AutoConfigureMockMvc` (see `StoreOpsApiTest`). Run all with `mvn verify`, one with `mvn test -Dtest=Class#method`.

## The rule
A status-code assertion alone is not a test of a business rule. Every GIVEN/WHEN/THEN in the sprint contract needs a test that asserts the outcome:
- Success path: the returned body or resulting state (`jsonPath("$.status").value("BLOCKED")`), and a follow-up GET proving the state persisted.
- Error path: status AND `jsonPath("$.code")` (`FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `VALIDATION_ERROR`), and that nothing changed afterwards.
- State change: an event was published. Use `@RecordApplicationEvents` + `ApplicationEvents` as in `ActivityEventsTest`, and assert both the event payload and that a rejected request published nothing.

## Models to copy
- `StoreOpsApiTest.deleteAllowedForOwnerOrStoreManagerOnly`: forbidden as stranger, forbidden with no header, allowed as owner, allowed as `s1` (STORE_MANAGER), then GET proves it is gone.
- `StoreOpsApiTest.addsMemberToProgrammeAndRejectsDuplicatesAndUnknownStaff`: every branch of one rule in one test.
- `ActivityEventsTest`: events on success, none on failure.

## Checklist for each new behaviour
- Any behaviour that changes state has an event test (`@RecordApplicationEvents`): the event payload on success, and no event on a rejected request. This is a hard gate (A-G6), not optional.
- At least one happy-path test asserting state, not just status.
- One test per distinct error code the Service can throw.
- Boundary data: unknown id, empty or null optional fields, duplicates.
- Seed data you can rely on: staff `s1` (STORE_MANAGER, store-1), `s2` (ASSOCIATE, store-1), `s3` (STORE_MANAGER, store-2). Activities and programmes are created by the test; stores are in-memory and shared across tests in the same Spring context, so filter or use unique values rather than assuming an empty list.
- When you change a domain value, grep tests for the old value.

## Do not
- Use `@MockBean` to bypass a rule under test.
- Weaken or delete an existing assertion to get green.
- Edit the surefire/Mockito `argLine` in `pom.xml`.
