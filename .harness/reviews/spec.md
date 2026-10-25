STATUS: APPROVED

# Spec: Bulk status update

## 1. Goal
A store manager can change the status of many activities with one `POST /api/activities/bulk-status` request instead of one PATCH per activity. The operation is all-or-nothing, and each activity whose status actually changes publishes one `ActivityUpdatedEvent`.

## 2. Scope and non-goals
In scope:
- New endpoint `POST /api/activities/bulk-status`, body `{ "activityIds": [...], "status": "<ActivityStatus>" }`, returns the updated activities (200).
- Permission: `X-Staff-Id` must identify a staff member with role `STORE_MANAGER`, otherwise `ForbiddenError`. A missing header or an unknown staff id is forbidden.
- Validation: `activityIds` non-empty and `status` present, otherwise 400 `VALIDATION_ERROR`.
- All-or-nothing: every ID is checked before anything changes. The first unknown ID (in request order) gives 404 `NOT_FOUND` naming it. Nothing changes and no event is published.
- Duplicate IDs count as one. Activities already in the target status are unchanged and publish no event.
- Events are published through `EventBus` only after all changes succeed.
- README endpoint table gets the new row.

Non-goals: per-store or per-programme selection, bulk change of priority/category/assignee, bulk delete, alerts reacting to the new events, pagination, any change outside the `activities` module (apart from the README row and tests).

## 3. Affected modules and gates
| Module | Change | Gates touched |
|---|---|---|
| `activities` | New request record, new controller route, new `ActivityService` method for the bulk update | G2 (`ForbiddenError`, `NotFoundError`, `ValidationError`), G3 (event publication), G4 (rules in Service, controller only routes and `@Valid`), G1 (uses `StaffReader` only) |
| `common.event` | No change. Reuses `ActivityUpdatedEvent(activityId, storeId, previousStatus, newStatus)`, statuses as enum names | G3, A-G6 |
| `staff` | No change. Uses `StaffReader.findStaff` | G1 |
| `alerts`, `programmes`, `reports` | No change | G5 untouched |

- New events: none. Existing `ActivityUpdatedEvent` is published once per activity that actually changes.
- New Reader methods: none. `StaffReader.findStaff` is reused.
- AppError use: `ForbiddenError` (403 `FORBIDDEN`), `NotFoundError("Activity", id)` (404 `NOT_FOUND`), `ValidationError` (400 `VALIDATION_ERROR`). No `ConflictError` path exists, because any status to any status is allowed.
- Gates for the diff: A-G6 requires `@RecordApplicationEvents` tests asserting payload and "no event on rejection". B-G2 requires business-outcome assertions. B4 requires the README row.

## 4. Sprints
1. Endpoint with rejections (403, 400, 404 all-or-nothing) and the success path: distinct IDs are changed, returned and one `ActivityUpdatedEvent` each is published.
2. Edge semantics: duplicate IDs treated as one, already-in-target-status activities unchanged with no event, mixed batches, plus the README row.

Both sprints end with `mvn -B verify` green. Sprint 1 is shippable on its own, but duplicate and no-op handling is only specified and guaranteed from sprint 2.

## 5. Open questions for the human
1. Check order. Bean validation on the body runs before the Service, so a request that is both invalid and from a non-manager returns 400, not 403. The plan accepts this ordering (consistent with the existing `@Valid` use on POST). Say so if you want 403 to win.
2. A status value that is not an `ActivityStatus` (for example `"FOO"`) is a body-parse error. The plan requires it to be 400 `VALIDATION_ERROR` only if `GlobalExceptionHandler` already does so. Changing the handler is in `common`, which is out of bounds. Is a different 400 body for this case acceptable?
