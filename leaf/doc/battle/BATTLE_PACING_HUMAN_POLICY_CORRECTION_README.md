# Installation note — Candidate B Main timing pilot

This changes **only the experimental Human-guided Candidate B policy** and adds research tests/documentation. v37.03 production Battle, Human Buy, Human Battle and game rules are not changed.

## Contract
- Normal Main actions still use Human Baseline's existing Main selector, effect-target selection and post-roll placement.
- Main timing now considers all unresolved rows rather than unconditionally consuming High and Middle tokens. Its expected-Draw tactical row proxy is documented as provisional in `BATTLE_PACING_HUMAN_BASELINE_AUDIT.md`.
- Two normal Main Actions per Battle, maximum one in an Engagement, maximum two Supports per Engagement, and Sunlight-funded Main behavior continue to be enforced by `ThreeEngagementBattle`.

## Verification
```
./gradlew compileSimulationKotlin
./gradlew simulationTest --tests '*EngagementMainTimingPlannerTest' --tests '*BattlePacingHumanWiringTest' --tests '*ThreeEngagementBattleTest' --tests '*BattlePacingABBatchTest'
 python3 bin/a03_verify_boundaries.py
```

Rerun a short *new* Human-guided pilot and inspect `opportunities.csv` Main allocation by row. Do not merge older A/B CSV summaries with new ones: this changes the Candidate B policy.
