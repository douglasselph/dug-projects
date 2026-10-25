# R3B.1a — Effect-aware Human Baseline decisions

## Why this increment exists

R3B.1 proved that research effects execute and their immediate mechanical output can be measured, but it did not ensure that the Human Baseline policy valued the *effective* effect. For example, experimental Root Four More could execute Raise +20 but still be valued and targeted partly as Raise +4. This made competitive calibration conservative and potentially misleading.

## Implementation

- The existing `DecisionContextFactory` already provides the effective, seat-specific `CreatureCardView.effect` / `PlantStackView.effect` through `Game.effectFor`. Keep this contract. The canonical card CSV is unchanged.
- `SimpleEffectMagnitude` supplies a central definition of the magnitudes of the uncomplicated experimental effects, reused by execution handlers and decision projections. This is not a general-purpose gameplay search engine.
- `CardScoringHelpers.playScore` now values research no-op as inert instead of retaining the printed card's attractive activation base; +20 immediate VP as a large, direct benefit; Raise +20 as the **actual capped best target gain**; and Draw 4 using available dice, projected Battle placement and purchase thresholds rather than assuming four free dice.
- `CardScoringHelpers.scoreDieTarget`, `HumanBaselineEffectStrategy`, `BattleEnabledPlantAnalyzer`, and `BattleOEdelweissAnalyzer` now recognize Raise +20 for target selection and Battle tactical projections; nested O Edelweiss projections recognize Draw 4.
- `HumanBaselineCardScorerRegistry` still resolves scorers by **card identity**, but the action score consumes the **effective effect identity** from the actor's decision context. Opponents see their own canonical effects in one-sided studies.
- The R3B.1 research launcher keeps its authoritative archive and resumability in `output/`, and additionally publishes the final `.tgz` atomically to `~/Downloads/` for upload.

## Verification

Run from the project root after extracting the changed-files ZIP:

```bash
./gradlew test --tests '*ResearchEffectDecisionAlignmentTest' \
  --tests '*ResearchCalibrationEffectsTest' \
  --tests '*HumanBaselineCardScorerRegistryTest'
python3 -m unittest discover -s bin/tests -p 'test_r3b1_effect_isolation.py' -v
python3 -m unittest discover -s bin/tests -p 'test_r3b_effect_sanity_gate.py' -v
bin/r3b1_effect_isolation --plan
```

The production `compileKotlin` task passed in the development container. The six new decision regression tests were independently compiled and executed against the compiled production classes: six passed. Three existing actual effect-execution tests were also independently compiled and executed: three passed (no-op, capped Raise +20, Draw 4). The full Gradle JUnit test task could not resolve several offline test-only libraries in that container, so **run the normal Gradle tests on Ubuntu**. The R3B and R3B.1 Python tests passed (3 + 4).

A smaller calibration run after the tests:

```bash
bin/r3b1_effect_isolation --generations 2 --population 4 --elites 1 \
  --games 8 --holdout-games 80 |& tee ~/Downloads/r3b1-effect-aware-decisions.log
```

This re-trains the six original-condition Buy-policy experiments and replays frozen, matched evaluations across shared-market and one-sided interventions. It is a calibration, not the full 36-card gate. The final line `UPLOAD THIS FILE:` names the copy in `~/Downloads`.

## Unresolved / do not overclaim

- A baseline heuristic that understands a +20 VP reward is not the same as a strategic optimizer that can discover all ways to exploit an effect. The trained learner still specializes primarily in Buy, with Human Baseline controlling most other decisions.
- A gain in hand dice or die pips is **not** intrinsically the same as a gain in final VP or win rate. The physical cap, supply and Battle capacity still matter. The projected Battle marginal utility of *several* draws is an approximation, not a mutating roll-out.
- The baseline will still not systematically plan several turns ahead, infer interactions among arbitrary combined effects, or optimize conditional card combos against adaptive opposition.
- Before overnight all-36-card screening, R3B.2 still needs per-activation opportunities/constraints, effect-use and contribution evidence, automated sensitivity checks, multiple strategy searches and counterstrategies. Unexercised or weakly tested effects must remain **Investigate**, not be classified safe.
- In one-sided experiments, only the target player's effect changes; untouched opponents retain official behavior. Matched initial seeds do not make divergent complete games mechanically identical.
- No official rulebook, card data, Grove setup, frozen weights, training architecture, or seed choices were altered in this increment.
