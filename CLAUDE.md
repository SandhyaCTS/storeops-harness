# CLAUDE.md: StoreOps harness orchestrator

Spring Boot 3.3 / Java 17 REST API (in-memory). This file runs a Planner / Generator / Evaluator / Monitor loop that adds features without breaking the architecture rules. Domain knowledge lives in `.harness/skills/`, not here.

## Commands
- `mvn -B verify`: Checkstyle (validate phase), all tests, SpotBugs. This is the build gate.
- `mvn -B test -Dtest=ArchitectureRulesTest`: the architecture gates G1-G5.
- `mvn spring-boot:run`: start on :8080.

## Entry prompt
A run starts when the human writes `PROMPT.md` and says `run harness`. `PROMPT.md` format:
```
# Feature: <name>
Goal: <one or two sentences>
Constraints: <rules, roles, limits>
Out of scope: <what not to build>
```

## Flow
1. **Plan.** Spawn the Planner (a fresh subagent told: "Read and follow `.harness/agents/planner.md`"). It writes `.harness/reviews/spec.md` (`STATUS: AWAITING APPROVAL`) and `sprint-N-contract.md` files. Stop and show the human the spec.
2. **Approval.** Nothing is built until the human types `APPROVED`. On `APPROVED`, change the first line of `spec.md` to `STATUS: APPROVED`. On any other reply, pass the feedback to the Planner and repeat step 1.
3. **Build loop, per sprint, in order.** Create branch `harness/<feature-slug>` once. Record `BASE` = current HEAD before the sprint starts. Then up to 3 iterations:
   1. Spawn the Generator (`.harness/agents/generator.md`). Give it the sprint number and iteration number.
   2. When it finishes, commit its changes: `git add -A src pom.xml README.md .harness/reviews .harness/output && git commit -m "sprint N iter K: <summary line>"`.
   3. Spawn the Evaluator (`.harness/agents/evaluator.md`) with the sprint, iteration and `BASE`.
   4. Read the first line of its new section in `sprint-N-evaluator-feedback.md` and route (below).
4. **Monitor.** After each sprint ends (any outcome), spawn the Monitor (`.harness/agents/monitor.md`) to write `sprint-N-run-log.md`. Pass it any token usage figures you have.
5. **Finish.** When all sprints pass, or a sprint escalates, report to the human.

## Routing on the Evaluator verdict
| Verdict | Action |
|---|---|
| PASS | Next sprint. |
| CONDITIONAL PASS | Next sprint. Append the findings and missed items to that sprint's contract under "Mandatory fixes carried forward". |
| FAIL | Back to the Generator with the same sprint, iteration + 1. The Generator reads the newest feedback section. |
| FAIL on iteration 3 | **Escalate.** Write `.harness/output/escalation.md` (sprint, the three verdicts, the unresolved findings with `file:line`, what the Generator tried, and a recommended human decision). Stop the loop. Do not start the next sprint. |

If a carried-forward finding is still unresolved when the final sprint is evaluated, that sprint is a FAIL (see `grading-criteria`).
If a verdict line is missing or not one of the three values, treat it as FAIL with reason `UNPARSEABLE-VERDICT` and do not count it as an iteration of the Generator's.

## Context reset
Every agent is a new subagent with no shared conversation. Each reads only the files its agent file lists, and communicates only by writing files in `.harness/reviews/`. The orchestrator passes only: sprint number, iteration number, `BASE`, and the agent file path. Never paste one agent's reasoning into another's prompt. The Evaluator never sees the Generator's chat, only its summary and the committed diff.

## Evaluator and CI
The Evaluator's gates are the same Maven commands CI runs (`mvn -B verify`, which includes `ArchitectureRulesTest`). The Evaluator runs them first and **feeds** CI: a sprint that passes the Evaluator should pass CI with the same result, and CI (`.github/workflows/ci.yml`, which runs `mvn -B verify` on every push to `main` and every pull request) stays as the independent backstop. The Evaluator adds what CI cannot: contract coverage, event-test coverage (A-G6), and the reading-based checklist.

## Artefacts (all in `.harness/reviews/` unless noted)
`spec.md`, `sprint-N-contract.md`, `sprint-N-generator-summary.md`, `sprint-N-evaluator-feedback.md` (one `## Iteration K` section per attempt, never overwritten), `sprint-N-run-log.md`, and `.harness/output/escalation.md` on escalation. Commit these with the code.

## Protected files
Only humans edit: `.harness/` (except files in `reviews/` and `output/`), `config/`, `src/test/java/com/storeops/architecture/ArchitectureRulesTest.java`, and the surefire/Mockito `argLine` and JaCoCo coverage thresholds in `pom.xml`. The Generator is forbidden to change them and the Evaluator fails any diff that does (A-G0).

## Skills
`.harness/skills/`: `app-context`, `architecture-principles` (shared); `sprint-decomposition` (Planner); `coding-conventions`, `how-to-test` (Generator); `how-to-review`, `grading-criteria` (Evaluator).
