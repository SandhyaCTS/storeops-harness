# Feature: Bulk status update
Goal: Let a store manager change the status of many activities in a single request, so end-of-day or end-of-campaign clean-ups don't need one PATCH per activity.

Constraints:
- Endpoint: `POST /api/activities/bulk-status` with body `{ "activityIds": [...], "status": "<ActivityStatus>" }`. Returns the updated activities.
- Permission: the `X-Staff-Id` header must identify a staff member with role `STORE_MANAGER`; otherwise `ForbiddenError` (403). A missing header is forbidden.
- All-or-nothing: validate every ID before changing anything. If any ID is unknown, respond 404 `NOT_FOUND` naming that ID; nothing is changed and no events are published.
- Validation: `activityIds` must be non-empty and `status` must be present (400 `VALIDATION_ERROR`). Duplicate IDs in the list are treated as one.
- Any status to any status is allowed (same as the single PATCH). An activity already in the target status is not changed and publishes no event.
- Every activity whose status actually changes publishes one `ActivityUpdatedEvent` (previous and new status) through `EventBus`, after all changes succeed.
- Keep all code in the `activities` module; reuse `StaffReader` for the role check. No changes to other modules.

Out of scope: per-store or per-programme bulk selection, changing priority/category/assignee in bulk, bulk delete, alerts reacting to the new events, pagination.
