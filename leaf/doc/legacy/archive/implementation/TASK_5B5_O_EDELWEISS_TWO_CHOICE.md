# Task 5B.5 — O Edelweiss two-choice planning

## Rule
Twice, optionally Play another **face-up** Plant (resolve its effect and spend it face down) or Flip another Plant either way. Resolve the first choice before the second. Done terminates early. The executor remains authoritative on legality and recursion prevention.

## Baseline scoring
The bounded planner enumerates initial Play / Flip / Done possibilities (excluding the active source) and scores each with the best legal follow-up. In the hypothetical continuation, facing changes are immutable: Play spends the selected Plant; Flip toggles it. After the actual first effect resolves the downstream strategy evaluates fresh legal choices again, rather than committing to a stale future choice. The same planner is used in Cultivation activation, Battle activation, and downstream choices in both phases.

A Play uses the existing Plant scorer for the relevant phase. Scores for both steps share one Done baseline (35), so the projected second step contributes only `second_score - 35`. A face-down-to-up Flip has a bounded future-option estimate (one quarter of the card's above-Done score); if the second step Plays the same card, that setup estimate is removed to avoid double counting. Face-up-to-down Flip is penalized based on the opportunity sacrificed. Stop (Done) is always available.

## Important precision limitation
This is **not yet full nested-effect state simulation**. Facing transitions are projected exactly; a played Plant's resource changes, dice values, Battle changes, and random expected draws are still evaluated by existing effect scorers, not materialized into the second step's snapshot. Consequently a second Plant that depends strongly on resources/dice changed by the first may still be misvalued. No future dice are sampled, no RNG is consumed, and the planner does not mutate gameplay state. Eligibility for complex nested effects is authoritative only in the executor, not the top-level estimated candidate set. The planner is deliberately bounded to two choices.

Future follow-up: project the selected nested Plant's consequences through the common state-projection infrastructure, then re-evaluate the second Plant from that richer projected state. Ensure the engine's legal choices and recursion guard are the final authority.

## Tests
Run `./gradlew test --tests '*HumanBaselineEffectStrategyOEdelweissBattleAlignmentTest'` followed by `./gradlew test integrationTest simulationTest`. The existing alignment test has been adjusted so the top-level score is allowed to exceed the best standalone first-step score when an additional choice adds value.
