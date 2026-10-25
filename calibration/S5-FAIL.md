## Iteration 1
VERDICT: FAIL

# Evaluator feedback: sprint 1, iteration 1

## Gate results
- A-G0 protected files: PASS. `git diff --name-only 1d6296c HEAD` touches only README.md, src/main (2 files), src/test (1 file), and `.harness/reviews/`.
- `mvn -o -B verify`: PASS. `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`; `BugInstance size is 0`; `BUILD SUCCESS`. (Checkstyle runs in validate and did not fail the build.)
- `mvn -o -B test -Dtest=ArchitectureRulesTest`: PASS. `Tests run: 6, Failures: 0, Errors: 0`; `BUILD SUCCESS`. (Static gates G1-G5 cannot see a missing publish.)
- A-G6 event coverage: FAIL. `ActivityService.complete` calls `repository.save` and publishes no event. No `@RecordApplicationEvents` test exists for it (`ActivityCompleteTest` has no such annotation).

## Checklist results
- A1: MISS. The status change publishes no event (contract C3, architecture-principles G3).
- A2: pass. The controller only routes (ActivityController.java:51-54); the logic is in the Service.
- A3: pass. The unknown id goes through `get(id)`, which throws NotFoundError (test asserts `$.code` NOT_FOUND).
- A4: pass. No new cross-module reads.
- B1: MISS. C3 maps to no test (generator summary says "see concerns", and concerns says "none").
- B2: pass for 404 (state-unchanged is trivially true).
- B3: pass for unknown id. A no-op complete of an already DONE activity is untested (not required by the contract).
- B4: pass. README.md row added at line 23.
- B5: pass. Uses `ActivityStatus.DONE`.
- B-G2: pass. Tests assert body and state via follow-up GET.

## Findings
- [GATE] A-G6 / G3 | src/main/java/com/storeops/activities/ActivityService.java:66-71 | `complete` saves the new status but never publishes `ActivityUpdatedEvent(id, storeId, previousStatus, DONE)` via `EventBus` (contract C3) | capture `current.status()` as previous, save, then `eventBus.publish(new ActivityUpdatedEvent(...))` after the save, mirroring `update` in the same class
- [GATE] A-G6 | src/test/java/com/storeops/ActivityCompleteTest.java:26-41 | no `@RecordApplicationEvents` test: neither one asserting the payload (previous TODO, new DONE) nor one asserting that the 404 publishes nothing | add both tests, modelled on `ActivityEventsTest`
- [CHECK] B1 | src/test/java/com/storeops/ActivityCompleteTest.java | C3 has no mapped test | covered by the fix above
- [CHECK] summary accuracy | .harness/reviews/sprint-1-generator-summary.md:3-4 | claims "Events added/published: ActivityUpdatedEvent on completion" but the code does not publish it, and "Concerns: none" is false | correct the summary next iteration

## Contract coverage
- C1 -> completeMarksDoneAndPersists -> yes (body status and follow-up GET)
- C2 -> completeUnknownIdIsNotFound -> yes (`$.code`)
- C3 -> none -> no (event not published and untested)

## NEEDS-HUMAN
None.
