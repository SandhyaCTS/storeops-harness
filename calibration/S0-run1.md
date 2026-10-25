## Iteration 1
VERDICT: FAIL

# Evaluator feedback: sprint 1, iteration 1 (BASE=60b0923)

## Gate results
- A-G0 protected files: FAIL. `git diff --name-only 60b0923...HEAD` lists `.harness/skills/app-context/SKILL.md` (protected; only `.harness/reviews/` and `.harness/output/` are allowed).
- `mvn -o -B verify`: PASS. `Tests run: 21, Failures: 0, Errors: 0`, `BugInstance size is 0`, `BUILD SUCCESS` (Checkstyle ran in validate and passed).
- `mvn -o -B test -Dtest=ArchitectureRulesTest`: PASS. `Tests run: 6, Failures: 0`, `BUILD SUCCESS` (A-G1 to A-G5).
- A-G6: N/A. The only new Service method, `countForStore`, does not save or delete, and the contract says no state changes.
- Note: `.harness/reviews/sprint-1-generator-summary.md` does not exist. I graded from the contract and the diff only.

## Checklist results
- A1 pass (no cross-module trigger).
- A2 pass. The blank check is in `ActivityService.java:40`; the controller only routes (`ActivityController.java:36`).
- A3 pass. Blank or missing storeId throws `ValidationError` (`ActivityService.java:40-41`), imported from `common.error`.
- A4 pass. No new cross-module read; it reuses `ActivityReader.countByStore`.
- B1 pass (see Contract coverage).
- B2 pass. VALIDATION_ERROR is tested for a missing and a blank storeId. There is no state to verify unchanged.
- B3 pass. Empty store, missing param and blank param are covered.
- B4 pass. README.md:20 and app-context were updated. The app-context edit is itself the G0 violation.
- B5 pass. No new domain strings.
- B-G1 pass. B-G2 pass (body fields and `$.code` asserted).

## Findings
- [GATE] A-G0 | .harness/skills/app-context/SKILL.md:22 (also line 22 of the Endpoints header, "Endpoints (13)") | Generator edited a protected `.harness/skills` file | Revert this file to its BASE content (`git checkout 60b0923 -- .harness/skills/app-context/SKILL.md`). Do not edit it; the human owns it. The README.md update is enough, and if the app-context change is needed, flag it for the human in the generator summary.
- [CHECK] process | .harness/reviews/sprint-1-generator-summary.md | the summary file is missing | write it.

## Contract coverage
- C1 -> ActivityCountTest.countsOnlyTheRequestedStore (ActivityCountTest.java:31) -> asserts storeId and count 2: yes.
- C2 -> same test, store-empty (ActivityCountTest.java:38) -> asserts count 0: yes.
- C3 -> ActivityCountTest.missingStoreIdIsAValidationError (ActivityCountTest.java:42) -> asserts 400 and `$.code` VALIDATION_ERROR for missing and blank: yes.

## NEEDS-HUMAN
- None. Code and tests are otherwise clean; the FAIL is solely A-G0.
