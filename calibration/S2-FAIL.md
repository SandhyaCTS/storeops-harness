## Iteration 1
VERDICT: FAIL

# Evaluator feedback: sprint 1, iteration 1 (BASE=1d6296c)

## Gate results
- A-G0 protected files: PASS. `git diff --name-only 1d6296c HEAD` lists only .harness/reviews/*, README.md, 3 main files, ActivityCountTest.java.
- `mvn -o -B verify`: PASS. "Tests run: 20, Failures: 0, Errors: 0, Skipped: 0", "BugInstance size is 0", "BUILD SUCCESS" (Checkstyle clean).
- `mvn -o -B test -Dtest=ArchitectureRulesTest`: PASS. "Tests run: 6, Failures: 0, Errors: 0", "BUILD SUCCESS" (A-G1..A-G5).
- A-G6: N/A. countForStore is read-only (no save/delete), contract states no events.
- B-G1: PASS.
- B-G2: FAIL. The only test for the new endpoint asserts status codes only.

## Checklist results
- A1 pass (no cross-module trigger). A2 pass (blank check in ActivityService.java:39-44, controller only routes). A3 pass (ValidationError at ActivityService.java:41). A4 pass (reuses ActivityReader.countByStore; no new cross-module read).
- B1 MISS: C1 and C2 have no test; the summary's named tests (countsOnlyTheRequestedStore, missingStoreIdIsAValidationError) do not exist. The only test is countEndpointResponds.
- B2 MISS: the 400 path asserts only status, not `$.code` = VALIDATION_ERROR.
- B3 MISS: no zero-count case, no blank storeId case, no other-store isolation.
- B4 pass (README.md:20 row added). B5 pass (no new domain strings).

## Findings
- [GATE] B-G2 | src/test/java/com/storeops/ActivityCountTest.java:21 | the 200 call asserts only status().isOk(); no body (storeId, count) is checked | assert `$.storeId` and `$.count` via jsonPath
- [GATE] B-G2 | src/test/java/com/storeops/ActivityCountTest.java:22 | the 400 call asserts only isBadRequest; `$.code` VALIDATION_ERROR is not checked | add `jsonPath("$.code").value("VALIDATION_ERROR")`
- [CHECK] B1 | src/test/java/com/storeops/ActivityCountTest.java:20 | C1 (2 in store-count, 1 in store-other -> count 2) is not tested; the seeded "store-1" is used with no count assertion | create 2 activities in store-count and 1 in store-other via POST, then assert count == 2
- [CHECK] B1/B3 | src/test/java/com/storeops/ActivityCountTest.java:20 | C2 (empty store -> 200, count 0) is not tested | add a test for an unused storeId asserting count 0
- [CHECK] B3 | src/test/java/com/storeops/ActivityCountTest.java:22 | blank storeId (`?storeId=`) not tested, though ActivityService.java:40 handles it | add a blank-param case asserting 400 and the code
- [CHECK] Summary accuracy | .harness/reviews/sprint-1-generator-summary.md:3 | names tests that do not exist | list the real test names

## Contract coverage
- C1 -> none -> no
- C2 -> none (countEndpointResponds only touches store-1, no count asserted) -> no
- C3 -> countEndpointResponds -> partial (status only, no `$.code`) -> no

## NEEDS-HUMAN
None.

Reason for FAIL: hard gate B-G2 failed (rule 1); B1, B2 and B3 are also missed (rule 5). Production code is correct and architecture-compliant; only the tests need fixing.
