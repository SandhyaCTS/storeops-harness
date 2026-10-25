# Design Brief: a harness for StoreOps

## Problem
A client's earlier AI experiment produced four kinds of bad code in a retail API: (1) imports reaching into another module's repository, (2) raw `throw new Error` instead of the typed `AppError`, (3) tests that checked HTTP status but not business rules, (4) state changes that skipped the event bus. The harness has one job: stop those four from reaching `main`, while letting Claude add features quickly.

## A. Intent decomposition

**From request to work items.** A human writes `PROMPT.md` (goal, constraints, out of scope). The Planner turns it into `spec.md` (marked `STATUS: AWAITING APPROVAL`) and one `sprint-N-contract.md` per sprint. Nothing is built until a human types `APPROVED`. The contract is the unit of intent: it is the only thing the Generator builds to and the only thing the Evaluator grades against.

**What a good sprint is.** Roughly one endpoint or one rule; ends with `mvn -B verify` green; every acceptance criterion is GIVEN/WHEN/THEN and checkable through MockMvc or a recorded event; every error code and the "no event on rejection" case are stated. For the demonstration feature (bulk status update, `POST /api/activities/bulk-status`) the Planner produced two sprints: (1) the endpoint with its rejections and success path, (2) duplicate IDs, no-op changes and the README.

**Example sprint contract (sprint 1, abridged).**
```
Goal: POST /api/activities/bulk-status rejects non-managers, invalid bodies and unknown IDs
without side effects, and otherwise sets the status on every listed activity, returns them
and publishes one ActivityUpdatedEvent per change.
Seed: staff s1 (STORE_MANAGER), s2 (ASSOCIATE). Activities A1, A2 in TODO.
1. GIVEN X-Staff-Id: s1 and {activityIds:[A1,A2], status:DONE} WHEN POSTed
   THEN 200, body lists A1, A2 as DONE in request order, and GET shows DONE for both.
2. GIVEN the request in 1 THEN exactly two ActivityUpdatedEvents (previousStatus "TODO",
   newStatus "DONE") are recorded, published only after both updates.
4. GIVEN X-Staff-Id: s2 (ASSOCIATE) THEN 403 FORBIDDEN, statuses unchanged, no event.
7. GIVEN a non-manager and an unknown activity ID THEN 403 (permission before IDs), not 404.
8. GIVEN s1 and activityIds:[A1,"missing",A2] THEN 404 NOT_FOUND naming "missing",
   A1 and A2 stay TODO, no event.
10. GIVEN s1 and activityIds:[] THEN 400 VALIDATION_ERROR, nothing changed, no event.
```
The full contract has 13 criteria; each maps to a test, and the Evaluator checks that mapping (checklist item B1).

**Where decomposition went wrong.** The Planner deferred the README row to sprint 2, which conflicted with rubric item B4 (README updated for any new endpoint). The Evaluator scored the sprint 84/100 for that alone. Decomposition has to respect the rubric, not only the feature (see the improvement in `REFLECTION.md`).

## B. Governance

**Skill file strategy.** Seven skills, each StoreOps-specific and read only by the agents that need it:
| Skill | Read by | Why it exists |
|---|---|---|
| `sprint-decomposition` | Planner | Sizing (one endpoint or rule per sprint), ordering by risk, GIVEN/WHEN/THEN rules, and the rule that no rubric item (README row, outcome test, event test) is deferred to a later sprint |
| `app-context` | all | The 5 modules, 13 endpoints, seeded staff, enums, the delete rule, the event flow |
| `architecture-principles` | all | Rules G1–G5, each with a StoreOps violation and fix |
| `coding-conventions`, `how-to-test` | Generator | Package-private constructors, records, enums not strings; tests assert outcomes and error codes, plus an event test for every state change |
| `how-to-review`, `grading-criteria` | Evaluator | The procedure, the commands, the finding format, the dimensions, gates and verdict rules |

The Generator deliberately does not read `grading-criteria` or `how-to-review`: it builds to the contract and the conventions, not to the grader. The seventh was added after the demonstration run, when the sprint 1 README miss showed the Planner needed its own sizing rules. Six is the minimum the brief asks for; I did not add more because every extra skill is another place for rules to drift apart (calibration found one such drift, below).

**Protected files.** `.harness/` (except `reviews/` and `output/`), `config/`, `ArchitectureRulesTest.java`, the surefire/Mockito `argLine` and the JaCoCo coverage thresholds in `pom.xml` can only be changed by a human. Gate A-G0 fails any diff touching them. Without it, the cheapest way for a Generator to get green is to loosen the gate; calibration scenario S3 (`LineLength` raised to 200) confirmed the Evaluator catches this and also notes that the green build ran against the loosened config.

**Relationship to CI.** The Evaluator's automated gates are the same Maven commands CI runs (`mvn -B verify`, which includes `ArchitectureRulesTest` and the coverage check), so the harness *feeds* CI and does not replace it. A sprint that passes the Evaluator should pass CI with the same result; `.github/workflows/ci.yml` stays as the independent backstop on every push to `main` and every pull request. The Evaluator adds only what CI cannot: contract coverage, event-test coverage (A-G6) and the reading-based checklist. Harness files live in `.harness/`, CI configuration in `.github/`, so changing one never touches the other.

**Audit trail.** Everything an agent decides is a file in `.harness/reviews/`, committed alongside the code: `spec.md`, `sprint-N-contract.md`, `sprint-N-generator-summary.md`, `sprint-N-evaluator-feedback.md` (one `## Iteration K` section per attempt, never overwritten), `sprint-N-run-log.md` (verdict, iterations, escalation flag, token estimate and its method) and, on escalation, `.harness/output/escalation.md`. Each iteration is its own commit (`sprint N iter K: ...`). A reviewer can replay a sprint from `git log` and read why each verdict was given, with `file:line` evidence.

**One StoreOps rule explained: state changes go through the event bus (G3, A-G6).** When module A's state changes and something else must react, A calls `EventBus.publish(new SomeEvent(...))`; the reacting module listens. Nobody calls another module's service or `ApplicationEventPublisher` directly. In StoreOps this is how `AlertService` learns of a new activity (`ActivityCreatedEvent`) without `activities` knowing `alerts` exists. The rule matters because a silent state change leaves alerts, reports and any future listener stale, and nothing crashes to tell you. Enforcement is two-layered. ArchUnit test `g3OnlyEventBusPublishesEvents` fails any class outside `common.event` that calls `publishEvent`, but a tool cannot see a *missing* publish. So gate A-G6 requires, for every changed Service method that saves or deletes, a `@RecordApplicationEvents` test asserting the event payload and that a rejected request publishes nothing. Calibration scenario S5 (a `complete` endpoint with no event, every Maven gate green) was caught by A-G6 alone. While building the harness I also found this rule already broken in the baseline: `PATCH` and `DELETE` on activities published nothing. I fixed them (`ActivityUpdatedEvent`, `ActivityDeletedEvent`) before the Evaluator ever ran, so the first run would not fail on code the Generator never touched.

## C. Non-determinism

**The problem.** The Evaluator is itself an LLM, so its judgement can vary between runs. The design lets it vary only where no tool can decide.

**Dimensions and weights.**
- **Architecture compliance, 60.** Four checklist items at 15: A1 cross-module triggers use event plus listener; A2 business rules sit in the Service; A3 each error path throws the right `AppError` subclass; A4 new cross-module reads go through a `*Reader`.
- **Correctness and test quality, 40.** Five items at 8: B1 each GIVEN/WHEN/THEN maps to a named test; B2 each error code is tested and nothing changed; B3 edge cases (unknown id, null/empty, duplicates); B4 README updated; B5 enums, not strings.

Architecture carries more because a boundary, error-contract or event-bus violation corrupts every later feature, while a missing test is cheap to add.

**Hard gates, and why none can be a soft check.** A gate is a command with a fixed exit result; if the Evaluator could weigh it against other things, a sprint with a boundary violation could still "average" into a pass.
| Gate | Command / check | Why it cannot be soft |
|---|---|---|
| A-G0 | `git diff --name-only <BASE>...HEAD` touches no protected file | A softened gate defeats every other gate |
| A-G1 | `ArchitectureRulesTest` `g1…`: no dependency on another module's Repository/Service/Controller; no cycles | A boundary violation is not a matter of degree |
| A-G2 | `g2OnlyAppErrorsAreConstructed` | Checkstyle's `IllegalThrows` only checks `throws` clauses, so it misses `throw new RuntimeException`; this is the only real check |
| A-G3, G4, G5 | `g3…`, `g4…`, `g5…` | Same: structural facts, not opinions |
| A-G6 | every state-changing endpoint has an event test | The one thing no static tool can see |
| B-G1 | `mvn -B verify` exits 0 (Checkstyle 0, all tests, SpotBugs 0; JaCoCo floors are separate gate B-G3) | A red build is a red build |
| B-G3 | JaCoCo `check` inside `mvn -B verify`: Service classes 80% line, Controller classes 70%, `common.*` 60%, overall 70% | Untested branches are how HTTP-only tests hide; the number comes from the tool, so the Evaluator cannot be lenient about it |
| B-G2 | each changed endpoint has a test asserting a business outcome beyond status code | Kills the "HTTP-only tests" failure mode |

**Verdict rules (in order).** Any failed gate is FAIL. Any definite miss on A1–A4 is FAIL. Score 100 is PASS. Score 84–99 is CONDITIONAL PASS. Below 84 is FAIL. 84 is 100 minus two correctness items, so a third correctness miss (76) or two undecidable architecture items (70) fails the sprint.

**Fallback when the Evaluator is unsure.** An item it cannot decide is neither a pass nor a miss: it earns no points, is listed under NEEDS-HUMAN with the evidence it did and did not have, and so can never produce a PASS. If a gate's command cannot be run or read, the verdict is FAIL with reason `GATE-UNVERIFIABLE`. If a verdict line is missing, the orchestrator treats it as FAIL (`UNPARSEABLE-VERDICT`). The design fails closed because a wrong PASS costs more than an extra human look.

**Worked verdict example (real, sprint 1 of the demonstration run).** Gates: `mvn -o -B verify` passed (25 tests, SpotBugs 0), `ArchitectureRulesTest` passed (6 tests), A-G6 satisfied (`BulkStatusTest` uses `@RecordApplicationEvents`, asserts payloads and zero events on rejection), no protected files touched. Checklist: A1–A4 pass (60/60). B1, B2, B5 pass (24). B3 NEEDS-HUMAN (no duplicate-ID test; the contract had put duplicates out of scope, and the Evaluator could not tell whether that excused it). B4 miss (`README.md` had no `bulk-status` row). Score 60 + 24 = **84/100, CONDITIONAL PASS**. The sprint advanced, and B3, B4 plus two test gaps were appended to the sprint 2 contract as mandatory fixes. Sprint 2 scored 100/100 and PASS.

**Escalation path.** A sprint gets three Generator iterations. A third consecutive FAIL makes the orchestrator write `.harness/output/escalation.md` (sprint, the three verdicts, unresolved findings with `file:line`, what the Generator tried, a recommended human decision) and stop; it does not start the next sprint. The Evaluator never softens a verdict to avoid escalation. **Honest limit:** in the demonstration run no sprint ever failed, so this path has never fired for real. It is specified and the FAIL routing was exercised only by seeded calibration defects.

**Calibrating the judge.** Before the graded run I ran the Evaluator in a fresh context on six branches with known defects (clean, raw exception, HTTP-only test, protected-file edit, repository-style boundary breach, missing event). It gave the expected verdict on all six. The first run of the clean branch returned FAIL, and the Evaluator was right: B4 told the Generator to update `app-context`, a protected file. That contradiction in my own rubric was fixed in `1d6296c` before the graded run.

## D. Architectural decisions

**D1. ArchUnit tests as the enforcement mechanism.**
- *Chosen:* ArchUnit rules in `ArchitectureRulesTest`, executed by `mvn test`.
- *Alternatives:* (a) Checkstyle rules alone; (b) `grep`/regex scripts for forbidden imports; (c) leave it to the Evaluator reading code.
- *Why:* Java has no depcruiser. Checkstyle's `IllegalThrows` does not see `throw new` statements, so (a) leaves the AppError rule unenforced. Regex (b) is brittle and cannot see method calls or cycles. (c) is exactly the soft judgement the brief warns about. ArchUnit inspects compiled classes, runs in the normal build (so CI keeps enforcing it after the harness is gone), and I proved each rule can fail by planting violations.
- *Assumption:* the module layout stays `com.storeops.<module>` with `*Repository`/`*Service`/`*Controller` naming. A rename silently weakens G1/G4, so the rules should be reviewed whenever naming conventions change.

**D2. Fresh-context subagents that talk only through files.**
- *Chosen:* each agent is a new subagent that reads only the files its agent file lists; the orchestrator passes only sprint, iteration, `BASE` and the agent path. The Evaluator never sees the Generator's reasoning.
- *Alternatives:* (a) one long session playing all roles; (b) a shared scratchpad all agents can read.
- *Why:* in a single session the "reviewer" has seen the author's excuses and drifts towards agreeing. Isolation is what let the Evaluator call the Generator's summary false in three calibration runs. The cost is that every handoff must be written down, which doubles as the audit trail.
- *Assumption:* the Generator and Evaluator, though the same model family, behave independently enough when their contexts differ. This is not guaranteed; correlated blind spots remain a risk, and a different model for the Evaluator is the obvious mitigation.

**D3. Hard gates plus a small score, instead of a pure score or a pure LLM judge.**
- *Chosen:* deterministic gates decide FAIL; a 60/40 checklist score only separates PASS (100), CONDITIONAL PASS (84–99) and FAIL; undecidable items fail closed.
- *Alternatives:* (a) one weighted score where gates are just heavy items; (b) a free-text "does this look good" verdict from the Evaluator.
- *Why:* (a) lets a high score offset a boundary violation. (b) is not reproducible and cannot be audited. The hybrid keeps the objective things objective and spends the LLM's judgement only on checklist items that no tool can check, each of which must cite evidence.
- *Assumption:* the checklist items are few and concrete enough that two Evaluator runs would agree. The weights (60/40) and the 84 threshold are my judgement, not derived; the consequence they encode is "an architecture miss is a failed sprint, up to two missed tests are not".

**D4. All-or-nothing bulk update instead of partial success.**
- *Chosen:* `POST /api/activities/bulk-status` validates every ID first; one unknown ID returns 404 and nothing changes, no event is published.
- *Alternatives:* (a) the brief's suggested partial-failure `PATCH` that updates the valid IDs and reports the rest; (b) best effort with a per-ID result list.
- *Why:* with the event bus, partial success means some `ActivityUpdatedEvent`s fire for a request that also returned an error, and listeners cannot tell whether the batch finished. All-or-nothing keeps "no event on rejection" a single testable rule (A-G6) and keeps each GIVEN/WHEN/THEN criterion binary. `POST` was chosen because the body is a command over a list, not a partial update of one resource.
- *Assumption:* store managers run batches small enough that rejecting the whole request is acceptable. If partial results are needed later, the contract must define per-ID outcomes and event ordering first.

## Known limits
- The coverage gate (B-G3) was added after the calibration runs and has not been exercised by a seeded defect; it is verified only by `mvn -B verify` passing today.
- A-G6 and checklist items A1–A4 rest on the Evaluator reading code; they are the weakest part of the design.
- G5 exempts records (plain data such as `Staff`) from "call only `*Reader` methods".
- Token figures in the run-logs come from reported subagent usage; the Planner's and the Monitor's own usage are not allocated to sprints.
- One feature, two sprints, one iteration each: enough to show the loop works, not enough to claim it is robust.
