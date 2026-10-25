# Reflection

## What worked
- **Deterministic gates did the heavy lifting.** `ArchitectureRulesTest` (ArchUnit, run by `mvn verify`) turned four of the client's failure modes into red tests. In calibration the Evaluator returned the expected verdict on all six seeded defects, each time citing `file:line` and the rule (for example `ActivityService.java:40`, G2).
- **The Evaluator did not trust the Generator.** In three calibration runs the fake summary lied (a green build that was red, tests that did not exist, an event that was never published) and the Evaluator caught it by running the commands itself.
- **Carry-forward worked.** Sprint 1 ended CONDITIONAL PASS (84/100). The four findings went into the sprint 2 contract, and sprint 2 passed 100/100 with all four resolved.

## What fell short
- **No FAIL and no escalation ever happened in the real run.** Both sprints passed in one iteration, so the 3-iteration loop and `escalation.md` are only tested by hand-built scenarios, not by a real Generator failing. One run of one feature is a weak sample.
- **The Planner and the rubric disagreed.** The sprint 1 contract deferred the README row to sprint 2, but rule B4 requires it for any new endpoint. The Evaluator correctly scored B4 a miss and left B3 undecided (NEEDS-HUMAN). That cost 16 points, about 16% of the score. Nothing was wrong with the code.
- **The same model family writes and grades.** Correlated blind spots are possible; the calibration used defects I planted, which proves the Evaluator catches known classes, not unknown ones.
- **Two checks rest on reading, not tools.** A-G6 ("every state change has an event test") is checked by the Evaluator inspecting the diff, and G5 exempts records. Both are weaker than the ArchUnit gates.

## One improvement, tied to what I saw
Add to `.harness/agents/planner.md`: *"Every sprint that adds or changes an endpoint must include its README row, and no checklist item in `grading-criteria` (B3, B4) may be deferred to a later sprint."* In this run, the Planner read `grading-criteria` but still split the README into sprint 2, which is exactly the 16-point loss above. The same kind of fault showed up in calibration scenario S0: B4 told the Generator to edit `app-context`, a protected file, so a faithful Generator would fail its own gate. I fixed that one (`1d6296c`); the Planner rule is the matching fix for the contract side. After making it, I would re-run the Planner on this PROMPT.md and check that sprint 1 contains the README row.
