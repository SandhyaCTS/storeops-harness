---
name: sprint-decomposition
description: How the Planner splits a StoreOps feature into sprints and writes testable GIVEN/WHEN/THEN criteria. Read before writing spec.md.
---

# Sprint decomposition

## Sizing and boundaries
- One sprint = one endpoint or one business rule, finishable by one Generator pass (target: under 10 files changed, one module).
- Every sprint ends with `mvn -B verify` green (Checkstyle, tests, SpotBugs, JaCoCo floors) and is shippable alone.
- Order by risk: first the path that touches permissions and error codes, then edge cases and side effects. A sprint that publishes events must contain the event test (A-G6) in the same sprint.
- Cross-module work splits at the module boundary: first the owning module adds a `*Reader` method, then the consumer uses it (G1). Never put both in one sprint if it risks a cycle.
- Do not defer a rubric item to a later sprint. Every sprint that adds or changes an endpoint carries its README row (B4), a happy-path outcome test (B-G2) and an event test for each state change (A-G6). Deferring these costs points the code did not earn (seen in the bulk-status run).

## Writing criteria
Each criterion is `GIVEN <seed data and header> WHEN <request> THEN <status, $.code, body field, state via GET, recorded event>`. It is testable when the THEN names something MockMvc or `ApplicationEvents` can assert. Reject wording such as "works correctly" or "handled properly".
Cover for every endpoint: success, each error code the Service can throw (`FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `VALIDATION_ERROR`), "nothing changed", and "no event on rejection". State the check order when two errors could both apply (e.g. permission before ID lookup).

## Example (bulk status, sprint 1)
GIVEN `X-Staff-Id: s2` (ASSOCIATE) and a valid body WHEN POSTed to `/api/activities/bulk-status` THEN 403, `$.code` = `FORBIDDEN`, statuses unchanged, no `ActivityUpdatedEvent` recorded.

## Contract skeleton
Goal, files expected to change, criteria, events (class and payload), "Mandatory fixes carried forward" (empty in sprint 1), out of scope.
