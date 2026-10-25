---
name: how-to-review
description: Step-by-step procedure and output format for the StoreOps Evaluator. Read before reviewing a sprint.
---

# How to review a sprint

You review evidence, not intentions. Do not read the Generator's reasoning; read `generator-summary.md`, the contract and the diff only.

## Procedure
1. Read `sprint-N-contract.md` and list its GIVEN/WHEN/THEN criteria.
2. Get the change: `git diff --stat <BASE>...HEAD` then `git diff <BASE>...HEAD -- src pom.xml README.md`; BASE is the commit before this sprint started and is given in your prompt. Fail immediately (G0) if the diff touches `.harness/` (other than `reviews/` and `output/`), `config/`, `ArchitectureRulesTest.java`, or the surefire `argLine` in `pom.xml`.
3. Run the hard gates in order and paste the key output lines into your feedback:
   1. `mvn -B verify` (Checkstyle, all tests, SpotBugs)
   2. `mvn -B test -Dtest=ArchitectureRulesTest`
   3. A-G6: for each changed Service method that saves or deletes, find the `@RecordApplicationEvents` test that covers it
4. Walk the checklists in `grading-criteria` for each dimension. For every item, either quote evidence (`file:line` or command output) or mark it failed.
5. Read the changed Service methods yourself for the things no tool proves:
   - Is each event published via `EventBus` after the success check, never before the permission check?
   - Does every error path throw an `AppError` subclass (watch `InternalError` import resolving to `java.lang.InternalError`)?
   - Is business logic in the Service rather than the Controller?
   - If under `reports`, does anything call a mutating method on another module?
6. Map each contract criterion to a named test. Open the test and confirm it asserts the outcome (body, `$.code`, state, event), not just status.
7. Decide the verdict with `grading-criteria`, then write `evaluator-feedback.md`.

## Finding format (one per issue)
```
- [GATE|CHECK] <rule id> | <file>:<line> | <what is wrong> | <fix the Generator should make>
```
Example: `[GATE] G1 | src/main/java/com/storeops/reports/ReportService.java:14 | injects ActivityService instead of ActivityReader | depend on ActivityReader; add the needed read method to it`

Rules:
- Every finding cites a real path and line. No "somewhere in the service".
- Name the rule from `architecture-principles` (G1-G5) or the checklist id.
- Say what to change, not just what is wrong.
- Do not fix code yourself. You are read-only on `src/`.

## Output file layout (`evaluator-feedback.md`)
```
# Evaluator feedback: sprint N, iteration K
VERDICT: PASS | CONDITIONAL PASS | FAIL
## Gate results   (command, result, key output line)
## Checklist results   (A1-A4, B1-B5: pass / miss / NEEDS-HUMAN, each with evidence)
## Findings   (format above, blocking first)
## Contract coverage   (criterion -> test name -> asserts outcome? yes/no)
## NEEDS-HUMAN   (items you could not decide, with why)
```
