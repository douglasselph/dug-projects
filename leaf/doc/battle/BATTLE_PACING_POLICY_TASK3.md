# Battle pacing — Task 3: Candidate B Support-continuation parity

Source baseline: `leaf(20261031-181838).tgz`, with Task 2 installed.

## Evidence from the supplied Task 2 pilot

The supplied four-player, final-Battle, ten-pair `summary.csv` reports 199
v37 Support actions and 191 Candidate B Support actions. Both executed 80
normal Mains. These are **real human-guided policy decisions**, but this is a
small exploratory sample and does not validate timing or balance outcomes.

## Defect before Task 3

`HumanGuidedEngagementPolicy` scored individual Supports using
`BattleSupportPriority` and retained only positive scores. It bypassed
v37's `BattleTurnOrchestrator`, whose B10-B14 flow checks continuation,
direct spending gates, cumulative Bee/Butterfly paths, enabling refresh
and Sunlight-funded Main Action value.

## Changes

- `EngagementSupportContinuation` (simulation only) calls the production
  `BattleTurnOrchestrator`, using only normal Mains committed to **this**
  Engagement. It does not pretend a deferred Main is available now.
- The inherited cumulative reachability can overcount Support resources
  when Candidate B has only 1–2 actions left. For that path only, use the
  production `BattleSupportReachability` on all feasible combinations of
  zero-to-two Bees and Butterflies that fit the *remaining* slots. A
  positive cumulative path must fit the current Engagement; it cannot
  borrow Support slots from another row.
- The existing `BattleSupportPriority`, Main/Support decision strategy,
  owned-card influences, Wisp hold/play decision, modular Battle Support
  policy and modular Battle Main policy still select actual choices.
- Legal Supports stay limited by the existing engine, and the scheduler
  enforces two Supports per player per Engagement. Sunlight consumes one
  slot and funds its Main without using a normal token.
- Recompute with fresh context after every executed action and after each
  resolved row. The normal Main timing plan is still fixed within a row.
- No production files or A03-protected boundaries were changed.

## Scope and cautions

This is a **targeted adaptation of v37's existing heuristics**, not proof
of equal competitive policy strength. In particular, the production B12
Water/Worm enabling calculation models an available Final Main; when a
normal Main is deferred to the next Engagement, its future value is not
fully simulated. Combining Water Refresh with Sunlight for a multi-step
future benefit may also remain undercounted. These require focused
rule/policy tests before final statistical acceptance. Wisp hold/play
remains an independent policy gate.

Main Action timing forecasts (Task 2) still use nonliteral tactical scores
and row-sensitivity projections; Task 3 does not change them.

## Acceptance run

```bash
unzip -o ~/Downloads/leaf_battle_pacing_policy_task3.zip -d ~/src/dug-projects/leaf
cd ~/src/dug-projects/leaf
python3 bin/a03_verify_boundaries.py
./gradlew compileSimulationKotlin
./gradlew simulationTest --tests '*EngagementSupportContinuationTest' --tests '*BattlePacingABBatchTest' --tests '*ThreeEngagementBattleTest'
bash doc/battle/run_battle_pacing_ab.sh --samples 10 --players 4 --battles 3 --policy human-guided --out output/experiments/battle-pacing/policy-task3-human-final-10
```

The direct-Java launcher compiles once and writes an archive to
`~/Downloads`. Verify complete pair counts, no `FAILURE.txt`, that real
Supports were used, and that every B opportunity has <=2 Supports and <=1
normal Main. Compare per-player/row distributions and trace anomalies;
**do not** infer real-world Battle time from JVM seconds.

## Next — Task 4: Battle rules-fidelity audit

Re-run seeded and stress scenarios for locked-row effects, immediate Wounds,
Doom, Wispquake, Butterfly, Water Refresh, Sunlight, and support chains.
Perform explicit parity tests that distinguish legal v37 choices from
Candidate B's new restrictions. Repair illegal effects before scaling
human-guided A/B simulations.
