# StoreOps REST API

Spring Boot 3.3 / Java 17 stub API for retail store operations. In-memory storage.

- `mvn test` – JUnit 5 + MockMvc tests (Checkstyle runs in `validate`, so it gates tests)
- `mvn verify` – tests + SpotBugs
- `mvn spring-boot:run` – start on :8080

## Modules (`com.storeops.*`)
Each has Controller (routes) → Service → Repository. `activities`, `programmes`, `staff`, `alerts`, `reports`.

Rules: no circular module imports; cross-module notification only via `common.event.EventBus`
(alerts reacts to `ActivityCreatedEvent`); `staff` (users) is exposed to others only as read-only `StaffReader`;
services/controllers throw only `AppError` subclasses (`common.error`), enforced partly by Checkstyle `IllegalThrows`.

## Endpoints
| Method | Path | Notes |
|---|---|---|
| GET | `/api/activities` | Optional filters: `programmeId`, `status` |
| GET | `/api/activities/{id}` | |
| POST | `/api/activities` | Publishes `ActivityCreatedEvent` |
| PATCH | `/api/activities/{id}` | Partial update; null fields unchanged |
| DELETE | `/api/activities/{id}` | `X-Staff-Id` header; owner or `STORE_MANAGER` only, else 403 |
| GET | `/api/programmes` | Optional filter: `storeId` |
| POST | `/api/programmes` | |
| POST | `/api/programmes/{id}/members` | 404 unknown programme/staff, 409 duplicate member |
| GET | `/api/staff` | |
| GET | `/api/alerts` | |
| POST | `/api/alerts/{id}/acknowledge` | |
| GET | `/api/reports/stores/{storeId}` | |
