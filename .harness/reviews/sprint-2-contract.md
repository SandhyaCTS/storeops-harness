# Sprint 2 contract: duplicates, no-op changes, README

## Goal
Duplicate IDs are treated as one and activities already in the target status are left untouched with no event, and the endpoint is documented in the README.

## Files expected to change
- `src/main/java/com/storeops/activities/ActivityService.java` (and the controller only if needed)
- tests under `src/test/java/com/storeops/activities/`
- `README.md` (endpoint table row)

## Acceptance criteria
Seed data: s1 (STORE_MANAGER). Activities A1, A2 with status TODO, A3 with status DONE.

Success
1. GIVEN s1 and `activityIds:[A1,A1,A1]`, status DONE WHEN POSTed THEN 200, the body contains A1 exactly once with DONE, and exactly one `ActivityUpdatedEvent` (TODO to DONE) is recorded.
2. GIVEN `activityIds:[A1,A2,A1]` THEN the body is [A1, A2] (first-occurrence order) and two events are recorded.
3. GIVEN `activityIds:[A3]`, status DONE (already DONE) THEN 200, the body contains A3 with DONE, A3 is unchanged, and no event is recorded.
4. GIVEN `activityIds:[A1,A3]`, status DONE THEN 200, the body contains both with DONE, and exactly one event is recorded (for A1, previousStatus "TODO").
5. GIVEN any-to-any transitions (DONE to BLOCKED, BLOCKED to TODO) THEN each succeeds with its event carrying the right previous and new status.

Rejections still hold with duplicates present
6. GIVEN `activityIds:[A1,A1,"missing"]` THEN 404 NOT_FOUND naming `missing`, A1 still TODO, no event.
7. GIVEN a non-manager and `activityIds:[A1,A1]` THEN 403 FORBIDDEN, A1 unchanged, no event.
8. GIVEN `activityIds:[]` THEN 400 VALIDATION_ERROR, no event.

README
9. The README endpoint table has a row for `POST /api/activities/bulk-status` noting the `X-Staff-Id` store-manager requirement, that it is all-or-nothing, and that it publishes `ActivityUpdatedEvent` per changed activity.

## State changes: event and payload
`ActivityUpdatedEvent(activityId, storeId, previousStatus, newStatus)` once per activity whose status really changes, none for no-ops or rejections, all published after every change has succeeded.

## Mandatory fixes carried forward
(Filled in by the orchestrator from sprint 1 evaluator findings, if any.)

## Out of scope
Anything outside the `activities` module (other than the README row), other bulk fields, bulk delete, alert listeners, pagination.
