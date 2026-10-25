---
name: app-context
description: What StoreOps is, its five modules, endpoints, domain values and the one event flow. Read before planning, coding or reviewing anything.
---

# StoreOps app context

Spring Boot 3.3 / Java 17 REST API for retail store operations. In-memory stores only (no database). Base package `com.storeops`.

## Modules and what each owns
| Module | Owns | Public surface other modules may use |
|---|---|---|
| `activities` | `Activity` (id, storeId, programmeId, title, status, priority, category, assigneeId, ownerId) | `ActivityReader.countByStore` |
| `programmes` | `Programme` with `ProgrammeMember` list | nothing |
| `staff` | `Staff` (id, name, storeId, role). Seeded: s1 Alice (STORE_MANAGER, store-1), s2 Bob (ASSOCIATE, store-1), s3 Carla (STORE_MANAGER, store-2) | `StaffReader` (`listStaff`, `listStaffByStore`, `findStaff`) |
| `alerts` | `Alert` (id, storeId, message, acknowledged) | nothing |
| `reports` | `StoreReport` (storeId, activityCount, staffCount), a cache only | nothing |
| `common` | `common.error` (AppError hierarchy), `common.event` (EventBus, DomainEvent, events) | everything |

Each module: `XController` (routes) -> `XService` -> `XRepository` (package-private, in-memory `ConcurrentHashMap`).

## Endpoints (12)
- `GET /api/activities` (filters `programmeId`, `status`), `GET /api/activities/{id}`, `POST /api/activities`, `PATCH /api/activities/{id}`, `DELETE /api/activities/{id}`
- `GET /api/programmes` (filter `storeId`), `POST /api/programmes`, `POST /api/programmes/{id}/members`
- `GET /api/staff`
- `GET /api/alerts`, `POST /api/alerts/{id}/acknowledge`
- `GET /api/reports/stores/{storeId}`

## Domain values (enums, never strings)
- `ActivityStatus`: TODO, IN_PROGRESS, DONE, BLOCKED (new activities start TODO)
- `ActivityPriority`: LOW, MEDIUM (default), HIGH, CRITICAL
- `ActivityCategory`: RESTOCKING, PLANOGRAM, AUDIT, COMPLIANCE, GENERAL (default)
- `ProgrammeRole`: STORE_MANAGER, DEPARTMENT_LEAD, ASSOCIATE
- `Staff.role` is a plain String; the only value the code checks is `"STORE_MANAGER"`.

## Business rules already in the code
- Delete an activity: allowed only if the `X-Staff-Id` header equals the activity's `ownerId`, or that staff member's role is `STORE_MANAGER`. Otherwise `ForbiddenError` (403, code `FORBIDDEN`). A missing header is forbidden.
- Add programme member: 404 if programme or staff unknown, 409 if the staff member is already a member.
- Error body for every failure: `ErrorResponse(code, message, statusCode)`.

## Event flow
`EventBus.publish(DomainEvent)` wraps Spring's `ApplicationEventPublisher`. Events (all in `common.event`): `ActivityCreatedEvent` (consumed by `AlertService.onActivityCreated`, which raises an Alert "New activity: <title>"), `ActivityUpdatedEvent` (activity id, store id, previous and new status) and `ActivityDeletedEvent`. The updated and deleted events have no listener yet.

## Commands
`mvn verify` (Checkstyle, all tests, SpotBugs) | `mvn test -Dtest=ArchitectureRulesTest` (the five architecture gates) | `mvn spring-boot:run` (port 8080).
