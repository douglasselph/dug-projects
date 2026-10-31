# Battle pacing: Task 2 — action-specific Main timing (research only)

## Scope

Only experimental simulation code changes. v37.03 canonical Battle, actual
Human Baseline action and target selection, A03 protected boundaries, the
3/2/2 round deck and launch architecture remain unchanged.

## Data flow

1. Legal `BattleMainAction` candidates come from production
   `BattleActionCoordinator.researchLegalMains` at the acting player's actual
   legal Engagement.
2. `BaselineMainActionForecaster` scores every supplied legal Draw, Plant,
   RoundEffect1, and RoundEffect2 using production
   `BattleFirstMainPriority` (first Main) or `BattleMainPriority.finalMainScore`
   (last Main), plus the same registered owned-card influence tags.
3. It scores each candidate against the actual immutable Battle context, then
   a no-open-row context, then each still-open row alone. The difference
   between each one-row and no-row score is a **heuristic tactical attribution**,
   clipped at zero; it is **not** a claim of literal extra VP.
4. Per-row maximum attributable value identifies up to two Engagements for
   normal Main opportunities. If two Mains remain and only Middle/Low are open,
   one Main must be spent in each if a legal Main exists.
5. That player's plan is frozen for their current Engagement and recomputed
   after the previous Strike has resolved, with locked rows omitted.
6. When a Main is committed, the actual v37 Human Baseline policy still chooses
   the action, and the production engine evaluates effect targets, dice rolls
   and placements at execution time. No action was pre-executed or selected by
   a separate timing policy.

## Known limits / next acceptance questions

- Action previews intentionally do **not** run effects or roll dice. They
  evaluate tactical scorer sensitivity, not the result of applying every card.
- Multi-row effects may be nonadditive. An intrinsic/fallback Plant scorer may
  be unable to attribute row benefit even when the effect is useful. These
  appear with a nonzero full score and zero row attribution. Inspect traces.
- If only non-row benefit is modeled, timing prefers earlier opportunities
  rather than assigning fictitious VP to a Strike.
- Current action choice is not *bound* to the action that gave a row its
  projection. This is intentional: v37 makes the final actual decision using
  live information, and target selection occurs after random outcomes.
- No new learned policy or C04 calibration was introduced.
- Test parity against meaningful Plant/Round situations in real seeded games;
  pure timing contract tests alone do not prove strategic equivalence.

## Verification

```bash
unzip -o ~/Downloads/leaf_battle_pacing_policy_task2.zip -d ~/src/dug-projects/leaf
cd ~/src/dug-projects/leaf
python3 bin/a03_verify_boundaries.py
./gradlew compileSimulationKotlin
./gradlew simulationTest --tests '*BaselineMainActionForecastTest' --tests '*EngagementMainTimingPlannerTest' --tests '*BattlePacingABBatchTest'
bash doc/run_battle_pacing_ab.sh --samples 10 --players 4 --battles 3 --policy human-guided --out output/experiments/battle-pacing/policy-task2-human-final-10
```

Result archives go to `~/Downloads` using the unchanged one-build/direct-Java
launcher. Inspect `main_plans.csv` and verify action-specific previews exist,
no locked row is credited, and the Main distribution is not hard-coded.

## Next: Task 3

Adapt the existing B10–B14 Human Baseline Support continuation and enabling-
Support planning to at most two Supports per Engagement, including Sunlight-
funded Main actions, with exact legal resource/target constraints and explicit
parity tests against the canonical Support policy.
