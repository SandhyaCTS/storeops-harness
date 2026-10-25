## Iteration 1
VERDICT: CONDITIONAL PASS

# Evaluator feedback: sprint 1, iteration 1

## Gate results
- A-G0 protected files: `git diff --name-only c23f6db HEAD` lists only the 3 activities main files, BulkStatusTest.java and the generator summary. PASS.
- `mvn -o -B verify`: "Tests run: 25, Failures: 0, Errors: 0, Skipped: 0", "BugInstance size is 0", "BUILD SUCCESS". Checkstyle runs in validate and did not fail the build. PASS (B-G1).
- `mvn -o -B test -Dtest=ArchitectureRulesTest`: "Tests run: 6, Failures: 0, Errors: 0", "BUILD SUCCESS". PASS (A-G1..G5).
- A-G6: changed Service method `ActivityService.bulkStatus` (ActivityService.java:68-86, calls `repository.save`). Covered by BulkStatusTest with `@RecordApplicationEvents` (BulkStatusTest.java:25). Payload asserted at :93-95 and :105-107; rejected requests assert zero events via `assertTodoAndNoEvents` (:61-66) and :124. PASS.
- B-G2: success, 403, 404, 400 tests assert body, `$.code`, follow-up GET state and events. PASS.

## Checklist results
- A1 (15) pass: no cross-module trigger added; event via EventBus (ActivityService.java:83).
- A2 (15) pass: role check, ID resolution and update loop are in the Service (:69-85); controller only has `@Valid` and routing (ActivityController.java:51-55).
- A3 (15) pass: ForbiddenError (:72) before ID lookup; `this::get` throws NotFoundError for the first unknown ID in request order; ValidationError via `@NotEmpty`/`@NotNull` (BulkStatusRequest.java:8) handled by the existing handler (tests assert VALIDATION_ERROR).
- A4 (15) pass: uses existing `StaffReader.findStaff`; no new cross-module read.
- B1 (8) pass: all 13 criteria map to named tests (see coverage).
- B2 (8) pass: 403, 404 and 400 tests assert unchanged TODO state and no events.
- B3 (8) NEEDS-HUMAN: unknown id, empty and null fields are covered; duplicates are explicitly out of scope in the contract (deferred to sprint 2), but B3 lists duplicates. Earns no points; I cannot decide whether the contract exclusion excuses it.
- B4 (8) miss: README.md has no `bulk-status` entry (grep finds nothing). The contract defers it to sprint 2, but B4 as written requires it for any new endpoint.
- B5 (8) pass: `ActivityStatus` enum used in the request record and Service; the event payload uses `.name()`, matching the existing event shape; no new status/role literals (the role check uses the existing `STORE_MANAGER` constant).

## Score
A 60/60, B 24/40 (B1, B2, B5 = 24; B3 NEEDS-HUMAN, B4 miss), total 84/100.

## Findings
- [CHECK] B4 | README.md (endpoint table) | no entry for `POST /api/activities/bulk-status` | add it to the README endpoint table (planned for sprint 2; carry forward as mandatory).
- [CHECK] B3 | src/test/java/com/storeops/activities/BulkStatusTest.java | no test for duplicate IDs | add a duplicate-ID test with the sprint 2 behaviour.
- Observation, non-scoring: ActivityController.java:52 `@Valid` runs before the Service role check, so a non-manager sending an invalid body gets 400 rather than 403. The contract does not specify this case. Decide in sprint 2 whether it matters.

## Contract coverage
1,2,3 -> managerUpdatesAllInOrderAndPublishesOneEventEach -> yes (order, status, unchanged fields, event payload)
4,5,6 -> nonManagerMissingHeaderAndUnknownStaffAreForbidden -> yes
7 -> permissionIsCheckedBeforeIds -> yes (403 and FORBIDDEN code, no events)
8,9 -> unknownIdIsNotFoundNamingFirstUnknownAndChangesNothing -> yes
10,11,12 -> invalidBodiesAreValidationErrorsAndChangeNothing -> yes
13 -> anyStatusToAnyOtherSucceeds -> yes
Minor: criterion 7 is tested with a missing ID only and does not assert statuses unchanged; there is no event assertion that publication happens only after both updates (the code does publish after the save loop, ActivityService.java:80-85).

## NEEDS-HUMAN
- B3: whether the contract's out-of-scope duplicate handling excuses the duplicates item. Evidence: contract "Out of scope" line; no duplicate test exists.
