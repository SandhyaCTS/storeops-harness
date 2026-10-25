## Iteration 1
VERDICT: PASS

# Evaluator feedback: sprint 2, iteration 1 (BASE=ee74bca)

## Gate results
- A-G0 `git diff --name-only ee74bca HEAD`: only `.harness/reviews/*`, `README.md`, `ActivityService.java`, `BulkStatusTest.java`. No protected file touched. PASS
- B-G1 `mvn -o -B verify`: `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`, `BugInstance size is 0`, `BUILD SUCCESS` (Checkstyle ran in validate, no violations). PASS
- A-G1..G5 `mvn -o -B test -Dtest=ArchitectureRulesTest`: `Tests run: 6, Failures: 0`, `BUILD SUCCESS`. PASS
- A-G6 Changed Service method `bulkStatus` (ActivityService.java:69-93) calls `repository.save` and publishes via `EventBus`. Event tests in BulkStatusTest assert payloads (`duplicateIdsCountOnce`, `duplicatesKeepFirstOccurrenceOrder`, `doneToBlockedAndBlockedToTodoCarryPreviousAndNewStatus`) and that rejections publish nothing (`assertTodoAndNoEvents` in the 403/404/400 tests). PASS
- B-G2 Endpoint tests assert body, `$.code`, follow-up GET state and recorded events. PASS

## Checklist results
- A1 pass: no new cross-module trigger; existing `EventBus.publish` of `ActivityUpdatedEvent` (ActivityService.java:91).
- A2 pass: dedupe, no-op and permission logic are in the Service (ActivityService.java:69-93); controller unchanged.
- A3 pass: 403 `ForbiddenError` (:73), 404 via `get` -> `NotFoundError`, 400 via `@Valid`; all tested.
- A4 pass: no new cross-module reads; staff accessed via `StaffReader`.
- B1 pass: every criterion maps to a named test (below).
- B2 pass: 403, 404, 400 each tested with state-unchanged and no-event assertions.
- B3 pass: duplicates, unknown id, empty list, null fields, already-in-target covered.
- B4 pass: README.md:23 row names `X-Staff-Id`, STORE_MANAGER only, all-or-nothing, duplicates, and `ActivityUpdatedEvent` per changed activity.
- B5 pass: `ActivityStatus` enum compared with `==` (ActivityService.java:80); `STORE_MANAGER` constant pre-existing.

## Score
A 60/60, B 40/40, total 100/100

## Findings
None blocking. All sprint 1 carried-forward items are resolved:
- B4 README row: README.md:23.
- B3 duplicate tests: `duplicateIdsCountOnce`, `duplicatesKeepFirstOccurrenceOrder`, `duplicatesWithUnknownIdAreNotFoundAndChangeNothing`.
- Non-manager with unknown id: `nonManagerWithUnknownIdIsForbiddenStatusesUnchangedNoEvent` asserts unchanged and no event.
- Publish-after-all-updates: `eventsArePublishedOnlyAfterEveryActivityIsUpdated` uses a probe listener that sees `[DONE, DONE]` at each event.

Non-blocking note: the Generator's noted concern (`@Valid` runs before the role check, so a non-manager with an invalid body gets 400) is not in the sprint 2 contract and is not graded.

## Contract coverage
1 -> duplicateIdsCountOnce -> yes
2 -> duplicatesKeepFirstOccurrenceOrder -> yes
3 -> alreadyInTargetStatusIsUnchangedWithNoEvent -> yes (snapshot equality, event count unchanged)
4 -> mixOfChangedAndAlreadyDoneEmitsOneEventForTheChangedOne -> yes
5 -> doneToBlockedAndBlockedToTodoCarryPreviousAndNewStatus -> yes
6 -> duplicatesWithUnknownIdAreNotFoundAndChangeNothing -> yes
7 -> nonManagerWithDuplicatesIsForbiddenAndChangesNothing -> yes
8 -> invalidBodiesAreValidationErrorsAndChangeNothing (includes `[]`) -> yes
9 -> README.md:23 -> verified by reading

## NEEDS-HUMAN
None.
