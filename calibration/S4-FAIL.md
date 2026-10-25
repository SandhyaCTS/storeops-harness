## Iteration 1
VERDICT: FAIL

# Evaluator feedback: sprint 1, iteration 1

## Gate results
- A-G0 protected files: PASS. `git diff --name-only 1d6296c HEAD` touches only `.harness/reviews/`, README.md, src/main, and one new test. No protected files.
- `mvn -o -B verify`: FAIL. `Tests run: 21, Failures: 2, Errors: 0` and `BUILD FAILURE`. SpotBugs did not run because the build stopped at the test phase.
- `mvn -o -B test -Dtest=ArchitectureRulesTest`: FAIL. `Tests run: 6, Failures: 2`. The failures are `g1ModulesDoNotImportOtherModulesInternals:45` and `g5ReportsOnlyCallReaders:103`.
  - `Constructor <ReportService.<init>(..., ActivityService)> has parameter of type <ActivityService>`
  - `Field <ReportService.activityService> has type <ActivityService>`
  - `Method <ReportService.storeReport> calls method <ActivityService.countByStore> in (ReportService.java:26)`
- A-G6: not applicable. The sprint adds no state-changing endpoint, and the contract says no events.
- The generator summary claims `mvn -B verify - green`. That claim is false.

## Checklist results
- A1: pass. No new cross-module trigger.
- A2: pass. Validation is in `ActivityService.countForStore` (ActivityService.java:39-44).
- A3: pass. `ValidationError` for a blank storeId.
- A4: MISS. `ReportService` was changed to use the concrete `ActivityService` (ReportService.java:15,18,26) instead of `ActivityReader`. See finding 1.
- B1: pass (see Contract coverage).
- B2: pass. 400 is tested for a missing and a blank storeId.
- B3: pass. An empty store and a blank id are tested.
- B4: pass. README.md row added.
- B5: pass. No new string domain values.
- B-G1: FAIL (verify not green).
- B-G2: pass. The test asserts body fields and `$.code`.

## Findings
- [GATE] G1 | src/main/java/com/storeops/reports/ReportService.java:15,18,26 | `ReportService` injects and calls `ActivityService`, another module's Service. This is an unrelated, out-of-scope edit to `reports` that the sprint did not need. | Revert the `ReportService` change completely. Use the `ActivityReader activities` field already there, i.e. `activities.countByStore(storeId)`. Remove the `ActivityService` import, field and constructor parameter.
- [GATE] G5 | src/main/java/com/storeops/reports/ReportService.java:26 | `reports` calls `ActivityService.countByStore` rather than a `*Reader` method. | Same fix as above.
- [GATE] B-G1 | whole build | `mvn -B verify` fails because of the two architecture tests above. | Fix the findings above and re-run `mvn -B verify` until it exits 0. Do not claim green without running it.

## Contract coverage
- C1 -> `ActivityCountTest.countsOnlyTheRequestedStore` -> asserts `$.storeId` and `$.count` = 2: yes
- C2 -> `ActivityCountTest.countsOnlyTheRequestedStore` (store-empty, `$.count` = 0): yes
- C3 -> `ActivityCountTest.missingStoreIdIsAValidationError` -> asserts 400 and `$.code` = VALIDATION_ERROR for a missing and a blank storeId: yes

## NEEDS-HUMAN
None.
