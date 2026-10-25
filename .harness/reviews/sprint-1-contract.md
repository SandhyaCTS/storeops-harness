# Sprint 1 contract: bulk-status endpoint, rejections and success path

## Goal
`POST /api/activities/bulk-status` rejects non-managers, invalid bodies and unknown IDs without side effects, and otherwise sets the status on every listed activity, returns them and publishes one `ActivityUpdatedEvent` per change.

## Files expected to change
- `src/main/java/com/storeops/activities/ActivityController.java`
- `src/main/java/com/storeops/activities/ActivityService.java`
- new `src/main/java/com/storeops/activities/BulkStatusRequest.java`
- new tests under `src/test/java/com/storeops/activities/` (MockMvc test and an event test modelled on `ActivityEventsTest`)

## Acceptance criteria
Seed data: staff s1 (STORE_MANAGER), s2 (ASSOCIATE). Activities A1, A2 exist with status TODO.

Success
1. GIVEN header `X-Staff-Id: s1` and `{activityIds:[A1,A2], status:DONE}` WHEN POSTed THEN 200 and the body lists A1 and A2 with `status` DONE, in request order, and follow-up `GET /api/activities/{id}` shows DONE for both.
2. GIVEN the request in 1 WHEN processed THEN exactly two `ActivityUpdatedEvent`s are recorded, one per activity, each with `activityId`, `storeId`, `previousStatus` "TODO" and `newStatus` "DONE", and they are published only after both activities are updated.
3. GIVEN only `priority`, `category`, `assigneeId`, `ownerId` and `title` of each activity WHEN bulk status runs THEN they are unchanged.

FORBIDDEN (403, `$.code` = `FORBIDDEN`)
4. GIVEN `X-Staff-Id: s2` (ASSOCIATE) WHEN a valid request is POSTed THEN 403 FORBIDDEN, statuses unchanged, no event.
5. GIVEN no `X-Staff-Id` header THEN 403 FORBIDDEN, statuses unchanged, no event.
6. GIVEN `X-Staff-Id: nobody` (unknown staff) THEN 403 FORBIDDEN, statuses unchanged, no event.
7. GIVEN a non-manager and an unknown activity ID WHEN POSTed THEN 403 (permission is checked before IDs), not 404.

NOT_FOUND (404, `$.code` = `NOT_FOUND`)
8. GIVEN s1 and `activityIds:[A1,"missing",A2]` WHEN POSTed THEN 404 NOT_FOUND, the message contains `missing`, A1 and A2 keep status TODO, and no event is recorded.
9. GIVEN several unknown IDs THEN the message names the first unknown ID in request order.

VALIDATION_ERROR (400, `$.code` = `VALIDATION_ERROR`)
10. GIVEN s1 and `activityIds:[]` THEN 400 VALIDATION_ERROR, nothing changed, no event.
11. GIVEN s1 and `activityIds` missing or null THEN 400 VALIDATION_ERROR, no event.
12. GIVEN s1 and `status` missing or null THEN 400 VALIDATION_ERROR, nothing changed, no event.

CONFLICT
13. No CONFLICT path exists for this endpoint. A test may assert that a status change from any value to any other (for example DONE to TODO) succeeds.

## State changes: event and payload
For each activity whose status changes: `ActivityUpdatedEvent(activityId, storeId, previousStatus, newStatus)` with enum names, published through `EventBus` only. Rejected requests (criteria 4-12) publish nothing. Business rules (role check, ID validation, change loop) live in `ActivityService`; the controller only routes and uses `@Valid`. The role check uses `StaffReader.findStaff` and the `STORE_MANAGER` role, as `delete` does.

## Mandatory fixes carried forward
(none, sprint 1)

## Out of scope
Duplicate-ID handling, "already in target status" no-op behaviour (sprint 2), README update (sprint 2), any change outside the `activities` module, other bulk fields, alert listeners for the events.
