## Iteration 1
VERDICT: FAIL

# Evaluator feedback: sprint 1, iteration 1

## Gate results
- A-G0 protected files: PASS. `git diff --name-only 1d6296c HEAD` touches only `.harness/reviews/`, README.md, and 4 files under src/.
- B-G1 `mvn -o -B verify`: FAIL. `Tests run: 21, Failures: 1, Errors: 1` and `BUILD FAILURE`. Key lines: `ArchitectureRulesTest.g2OnlyAppErrorsAreConstructed:63 ... Method <ActivityService.countForStore(String)> calls constructor <java.lang.IllegalStateException.<init>(String)> in (ActivityService.java:40)` and `ActivityCountTest.missingStoreIdIsAValidationError:43 » Servlet Request processing failed: java.lang.IllegalStateException: storeId is required`.
- `mvn -o -B test -Dtest=ArchitectureRulesTest`: FAIL. `Tests run: 6, Failures: 1`, same G2 violation (A-G2). G1, G3, G4, G5 pass.
- A-G6: N/A. The change is read-only (count); no save/deleteById in changed Service methods.

## Checklist results
- A1: pass (no cross-module trigger).
- A2: pass. Validation is in `ActivityService.countForStore`; the controller only routes.
- A3: MISS. The blank storeId path throws IllegalStateException instead of ValidationError (ActivityService.java:40).
- A4: pass (`countByStore` already on ActivityReader).
- B1: C1 and C2 map to `countsOnlyTheRequestedStore`, which asserts the outcome. C3 maps to `missingStoreIdIsAValidationError`, which asserts the outcome but fails.
- B2: miss. The test exists but fails, so the 400 code is not delivered.
- B3: pass (empty store and blank id are covered).
- B4: pass. README.md row added.
- B5: pass (no new domain strings).

## Findings
- [GATE] G2 | src/main/java/com/storeops/activities/ActivityService.java:40 | `throw new IllegalStateException("storeId is required")` constructs a non-AppError; it yields a 500/unhandled exception instead of 400 VALIDATION_ERROR | throw `new ValidationError("storeId is required")` (import com.storeops.common.error.ValidationError)
- [GATE] B-G1 | src/test/java/com/storeops/ActivityCountTest.java:43 (missingStoreIdIsAValidationError) | fails with the IllegalStateException above; contract C3 not met | fixed by the G2 fix; rerun `mvn -B verify`

## Contract coverage
- C1 -> countsOnlyTheRequestedStore -> yes (storeId and count in body); passes in isolation of the failing test
- C2 -> countsOnlyTheRequestedStore (store-empty, count 0) -> yes
- C3 -> missingStoreIdIsAValidationError -> yes (asserts `$.code`), but FAILS in the current code

## Summary-claim check
The summary says "mvn -B verify - green". This is false: verify fails.

## NEEDS-HUMAN
None.
