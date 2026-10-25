---
name: grading-criteria
description: Dimensions, hard gates, checklists, verdict rules and ambiguity fallback for StoreOps sprints. Read when deciding a verdict.
---

# Grading criteria

Two dimensions. Hard gates are deterministic commands: one failure fails the sprint. Checklists cover what no tool can prove; they are judged by reading and must cite evidence.

Weights: **Architecture compliance 60, Correctness and test quality 40** (sum 100). Architecture carries more because a boundary, error-contract or event-bus violation corrupts the design for every later feature, while a missing test is cheap to add. Points are earned per checklist item (A: 4 x 15, B: 5 x 8); hard gates are separate and are not scored.

## Dimension A: Architecture compliance (weight 60; a miss here = FAIL)
Hard gates (run `mvn -B test -Dtest=ArchitectureRulesTest` plus the checks below; any failure = FAIL):
- A-G0 Protected files untouched: `.harness/` (except `.harness/reviews/` and `.harness/output/`), `config/`, `ArchitectureRulesTest.java`, surefire `argLine` in `pom.xml` (check with `git diff --name-only <BASE>...HEAD`).
- A-G1 `g1ModulesDoNotImportOtherModulesInternals`, `g1NoCyclesBetweenModules`: no dependency on another module's Repository/Service/Controller; no cycles.
- A-G2 `g2OnlyAppErrorsAreConstructed`: only `AppError` subclasses are constructed.
- A-G3 `g3OnlyEventBusPublishesEvents`: only `EventBus` calls `publishEvent`.
- A-G4 `g4LayersAreSeparated`: Controller -> Service -> Repository.
- A-G5 `g5ReportsOnlyCallReaders`: reports only call `*Reader` methods.
- A-G6 Event coverage: every endpoint in the diff that changes state has a test using `@RecordApplicationEvents` (model: `ActivityEventsTest`) that asserts the event payload, and one asserting a rejected request publishes nothing. Check by listing changed Service methods that call `repository.save` or `deleteById` and finding their event test. No such test = FAIL. (This turns "state changes use the event bus" from a judgement into a check.)

Checklist (judged by reading; 15 points each):
- A1 A new cross-module trigger is an event plus `@EventListener` in the reacting module, not a direct call.
- A2 Business rules sit in the Service; controllers only route and `@Valid`.
- A3 Each error path throws the right subclass: 404 `NotFoundError`, 409 `ConflictError`, 400 `ValidationError`, 403 `ForbiddenError`.
- A4 New cross-module reads are added to a `*Reader` interface; no Repository made public.

## Dimension B: Correctness and test quality (weight 40; misses can be carried forward)
Hard gates:
- B-G1 `mvn -B verify` exits 0 (Checkstyle 0 violations, all tests pass, SpotBugs 0 bugs, JaCoCo floors met; see B-G3).
- B-G3 Coverage floors (JaCoCo `check`, part of `mvn -B verify`): Service classes 80% line, Controller classes 70%, `common.*` shared utilities 60%, overall 70%. A "Rule violated" line in the verify output = FAIL. Deterministic: the numbers come from the tool, not from the Evaluator.
- B-G2 Every endpoint added or changed in the diff has at least one test asserting a business outcome beyond status code (body field, `$.code`, state via follow-up GET, or recorded event).

Checklist (8 points each):
- B1 Each GIVEN/WHEN/THEN in the sprint contract maps to a named test that asserts its outcome.
- B2 Every error code the new code can throw has a test, and the test asserts nothing changed.
- B3 Edge cases covered: unknown id, null or empty optional fields, duplicates.
- B4 README endpoint table updated for any new or changed endpoint. (`app-context` is protected and updated by a human after the run.)
- B5 Domain values use enums; no new string literals for status, priority, category or role.

## Score and verdict (apply in order)
Score = sum of points for items that passed (0-100). An item is all-or-nothing. An item marked NEEDS-HUMAN earns no points.
1. Any hard gate failed (A or B) -> **FAIL**, whatever the score.
2. Any A item definitely missed -> **FAIL**.
3. Score 100 -> **PASS**.
4. Score 84-99 -> **CONDITIONAL PASS** (for example up to two B misses, or one undecidable A item at 85).
5. Score below 84 -> **FAIL**.
Why 84: it is 100 minus two B items (16). A third B miss (76), or two undecidable A items (70), is a failed sprint.

## CONDITIONAL PASS routing
The sprint advances. Each missed item and finding is copied into the next sprint's contract as a mandatory fix. If a carried-forward finding is still unresolved in the final sprint, that sprint is a **FAIL**.

## Ambiguity fallback
If the evidence does not let you decide an item (tool output truncated, a flaky test, a rule readable two ways):
- do not count the item as passed and do not count it as a miss; list it under NEEDS-HUMAN with the evidence you did and did not have,
- the item earns no points, so the verdict cannot be PASS (see the score rules).
If a hard gate's command cannot be run or its output is unreadable, the verdict is FAIL with reason `GATE-UNVERIFIABLE`.
If you are unsure whether you are unsure, treat it as ambiguous.

## Iteration limit
A sprint gets 3 Generator iterations. A third consecutive FAIL escalates to `.harness/output/escalation.md`. The Evaluator never softens a verdict to avoid escalation.
