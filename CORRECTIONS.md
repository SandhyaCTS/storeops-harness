# Phase 1 Corrections Log

Every fix I made to the generated StoreOps scaffold (baseline `d5199d6` -> `f7e3e1e`), and the harness rule each one becomes.
Entries marked DRAFT are inferred from the diff; confirm the "what went wrong" wording from memory before submitting.

| # | What the scaffold got wrong | Fix | Harness rule it becomes | Skill file |
|---|---|---|---|---|
| 1 | Tests failed: Mockito could not self-attach at runtime on macOS JDK 17. | `pom.xml`: added `mockito.version`, `maven-dependency-plugin` (`properties` goal at `validate`), and a surefire `argLine` of `-javaagent:${org.mockito:mockito-core:jar} -Xshare:off`. | Generator must not touch the surefire/mockito setup in `pom.xml`. Run tests only through `mvn test`, never a bare IDE runner. | coding-conventions, how-to-test |
| 2 | Endpoint set did not match the spec. Activities lacked `PATCH /{id}` and `DELETE /{id}`, and programmes lacked `POST /{id}/members`. | Added all three, plus the list filters `programmeId` and `status` (activities) and `storeId` (programmes). | A new endpoint needs a Controller method, a Service method and a MockMvc test in the same sprint. The README endpoint table and `CLAUDE.md` must be updated with it. | app-context, how-to-test |
| 3 | Domain models were too thin. `Activity.status` was a raw `String` with an `"OPEN"` literal, with no priority, category, assignee or owner. | Introduced the enums `ActivityStatus` (TODO, IN_PROGRESS, DONE, BLOCKED), `ActivityPriority` and `ActivityCategory`, and widened the records. Defaults are MEDIUM and GENERAL. | Status, priority and category are enums, never strings. Nullable request fields have explicit defaults in the Service. | coding-conventions |
| 4 | No 403 path. Delete authorisation had no `AppError` to throw. | Added `ForbiddenError extends AppError` (code `FORBIDDEN`, 403). `ActivityService.delete` allows only the owner or a `STORE_MANAGER`. | Authorisation failures throw `ForbiddenError`. They never return a status from the Controller and never throw `IllegalStateException` or `RuntimeException`. This is the typed-AppError rule. | architecture-principles, how-to-review |
| 5 | The Activities module needed staff roles but had no sanctioned way to read them. | Extended the read-only `StaffReader` with `findStaff(id)`. `ActivityService` and `ProgrammeService` depend on `StaffReader`, never on `StaffRepository`. | Cross-module reads go through a `*Reader` interface only. An import of another module's `*Repository` is an automatic FAIL (depcruiser-equivalent: ArchUnit or an import grep). | architecture-principles, how-to-review |
| 6 | Seed data used the role `"MANAGER"`, which clashed with the `STORE_MANAGER` role the delete rule checks. | `StaffRepository` seeds changed to `STORE_MANAGER`. | Role names are shared constants. Check seed data against rules when you add a role check. | app-context |
| 7 | Business-rule paths (duplicate member, unknown staff, unknown programme) were untested. | Added `ConflictError` and `NotFoundError` paths in `ProgrammeService.addMember`, and 3 tests that assert error codes and states as well as HTTP status. | Every business rule needs a test that asserts the rule's outcome (error code, resulting state), not just `status().isOk()`. This is the "tests check HTTP only" failure mode. | how-to-test, grading-criteria |
| 8 | An existing assertion hard-coded the old `"OPEN"` status. | Updated the test to `"TODO"`. | When you change a domain value, grep the tests for the old value. | how-to-test |
| 9 | `CLAUDE.md` did not exist, so Claude had no architecture guidance. | Generated `CLAUDE.md` with commands and cross-module rules. | This file becomes the base for the orchestrator `CLAUDE.md`. Split architecture content into skills and keep orchestration in `CLAUDE.md`. | architecture-principles |

## Known gaps to resolve before Phase 2

- **README endpoint table:** it lists 8 rows. The code now exposes 12 mappings, so the README is stale.
- **Event bus coverage:** only `ActivityCreatedEvent` exists. `PATCH` changes activity status without publishing an event. This is exactly the "state change skipped the event bus" failure mode. Decide whether status changes should publish an event. It is the natural first rule for the Evaluator.
- **Reports module:** `ReportService` should be checked to confirm it only reads through `ActivityReader` and `StaffReader`, and has no write methods.
- **SpotBugs:** `mvn verify` failed in my sandbox on a site-descriptor download (network). Confirm it passes locally.
- **App smoke test:** run `mvn spring-boot:run` and `curl localhost:8080/api/activities`, then record the result here.
