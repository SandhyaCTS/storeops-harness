# Journal

A factual log of how the harness was built and run, drawn from the commit history and the run artefacts. Calibration outputs are in `calibration/`.

## Phase 1: scaffold (Oct 20-21)
- Generated StoreOps (Spring Boot 3.3, Java 17, Maven) from the bootstrap prompt; committed as the baseline (`d5199d6`).
- First corrections (`f7e3e1e`): Mockito could not self-attach on macOS (fixed with a surefire `-javaagent` argLine), the endpoint set did not match the spec (added `PATCH`/`DELETE` on activities and `POST /{id}/members`), status/priority/category became enums, `ForbiddenError` added, and seed role `MANAGER` became `STORE_MANAGER`.
- `mvn verify` failed in the AI sandbox on SpotBugs (a blocked site-descriptor download) but passed on my machine. Lesson: a failing tool is not always a failing build; check where it ran.
- Wrote the corrections down in `CORRECTIONS.md` (10 entries), each mapped to the skill it should feed. This became the raw material for the skills.

## Phase 2: harness (Oct 21-25)
**Finding before writing anything.** Checkstyle's `IllegalThrows` only checks `throws` clauses, not `throw new RuntimeException(...)`. The AppError rule had no real gate. Java also has no depcruiser. Decision: ArchUnit tests inside `mvn test`, one test per rule (G1-G5). I planted violations to confirm every rule can fail: all six tests went red with readable messages.

**Second finding.** `PATCH` and `DELETE` on activities changed state without publishing events, so the baseline already broke the event-bus rule. Decision: fix it now (`ActivityUpdatedEvent`, `ActivityDeletedEvent`, `ActivityEventsTest`) rather than list it as an exception, so the first Evaluator run would not fail on code nobody generated.

**Grading design.** First draft: 55/45 weights with 90/70 thresholds. Then I weighed it: those numbers were arbitrary, and the one check I cared most about (state changes publish events) was judged by reading code, which is soft. Changes: turned it into a hard gate (A-G6: an event test for every state change), made a definite architecture miss an automatic FAIL, and made undecidable items fail closed (NEEDS-HUMAN, never PASS). I briefly dropped numeric scores for a miss-counting rule, then restored them because the brief asks for weighted dimensions: 60/40, with 15 points per architecture item and 8 per correctness item, so the arithmetic is whole numbers. Threshold 84 = 100 minus two correctness items.

**Calibration (before the graded run).** Ran the Evaluator, as a fresh subagent, on six branches with one known defect each:
| Scenario | Planted defect | Verdict |
|---|---|---|
| S0 run 1 | none | **FAIL (unexpected)** |
| S0 run 2 | none, after fix | PASS |
| S1 | `throw new IllegalStateException` | FAIL, A-G2, `ActivityService.java:40` |
| S2 | test asserting only `status().isOk()` | FAIL, B-G2 |
| S3 | `LineLength` raised in `config/checkstyle.xml` | FAIL, A-G0 |
| S4 | `reports` injecting `ActivityService` | FAIL, G1/G5 |
| S5 | `complete` endpoint, no event, all Maven gates green | FAIL, A-G6 |

The surprise was S0. The clean branch failed because rule B4 told the Generator to update `app-context`, which sits in the protected `.harness/` folder. A faithful Generator would have failed its own sprint. I fixed the rubric (`1d6296c`) and re-ran. This is the strongest argument for calibrating the judge before trusting it. Also useful: in S1, S2 and S5 the fake Generator summary lied (a green build that was red, tests that did not exist, an event never published) and the Evaluator caught each by running the commands itself. The optional S6 (ambiguity fallback) was skipped, so that path is untested.

## Phase 3: the run (Oct 25)
- Feature: bulk status update. I chose it and its rules via questions: all-or-nothing, `STORE_MANAGER` only, any status to any status.
- The Planner produced a spec and two sprints. I reviewed it, noted two things (a gap between sprints on duplicate/no-op handling; permission checked before IDs), and typed `APPROVED`.
- **Sprint 1:** one iteration, CONDITIONAL PASS, 84/100. B4 missed (README deferred by the contract) and B3 undecided (duplicates out of scope in the contract). Both findings plus two test gaps went into the sprint 2 contract.
- **Sprint 2:** one iteration, PASS, 100/100, all four carried findings resolved.
- Token usage (reported): sprint 1 64,565, sprint 2 65,566, Planner 29,013 shared. Monitor usage not included.
- Merged to `main` as `0d37e5a`; `mvn verify` green (34 tests, SpotBugs 0).
- The Planner's deferral of the README against rule B4 is the clearest example of the Planner and the rubric disagreeing; it is the improvement named in `REFLECTION.md`.

## Phase 4: deploy and write-up (Oct 25)
- Docker was not available inside the AI sandbox and the app would not start there, so the live check was not the AI's to claim. I built the image and ran the three `curl` checks on my own machine and pasted the real output into `DEPLOYMENT.md` (200 for the manager, 403 `FORBIDDEN` for the associate, 404 for an unknown ID).
- Chose a runtime-only Docker image (prebuilt jar) to avoid Maven downloading inside Docker behind the company proxy. Trade-off: `mvn verify` must run first.
- AI drafted `REFLECTION.md` and `DESIGN_BRIEF.md` from the artefacts; the rationale in section D and the weights are decisions I have to be able to defend.

## What I would do differently
- Run the real loop on a second, deliberately harder feature, so a genuine FAIL and the escalation path are exercised once.
- Apply the Planner rule from `REFLECTION.md` and re-run the Planner on the same prompt to confirm the README lands in sprint 1.
- Try a different model for the Evaluator to test the correlated-blind-spot risk.
- Exercise the ambiguity fallback (S6).

## Honest limits
- One feature, two sprints, one iteration each. Enough to show the loop runs; not proof it is robust.
- No sprint FAILed in the real run, so the 3-iteration escalation has never fired outside hand-built scenarios.
- A-G6 and checklist items A1-A4 rely on the Evaluator reading code.
