# Battle pacing — Task 6: duration and passive downtime

Implementation base: the source archive supplied for Task 6 on 2026-10-31. Task 6 extends the existing Task-5 paired legal-opening experiment; it does **not** replace the game engine, canonical v37 Battle scheduler, Human Baseline policies, Candidate-B Main planner, or B10–B14 Support continuation logic.

## Research question

Task 6 asks whether Candidate B's three Engagements are likely to make Battle noticeably shorter and reduce player-experienced waiting, especially in a four-player final Battle, without pretending simulator CPU time is human deliberation time. Every reported minute is therefore a **human-time model estimate**, derived from simulator-observed events and adjustable assumptions. It must later be calibrated against timed physical playtests before being treated as measured tabletop time.

The attached authoritative Task-5 natural 100-pair results already established a mechanical pacing signal: Candidate B used fewer Supports in every cohort. In four-player Battle 3, A used 2,046 Supports and B used 1,779: 5.115 versus 4.448 per player, about a 13% reduction. That observation alone did not establish elapsed time or downtime, which is the gap Task 6 fills.

## Smallest measurement architecture

Canonical A remains scheduled by `BattleActionCoordinator`. A new optional `collectResearchPacing=false` argument records read-only decision-window snapshots only when the experiment asks for them. The production default is false. The same production legal-action lists and production action executors are still authoritative. A deterministic simulation test runs two identical canonical-A copies, one instrumented and one not, and requires identical action/Strike/Doom/VP outcomes.

Candidate B keeps its existing one-opportunity-per-player-per-Engagement scheduler. Each opportunity now records its sequential Main/Support/Pass decisions with the same before/after snapshot vocabulary used by A. No Candidate policy choice is added or removed for Task 6.

Each observed decision step retains: actor; legal Main and Support counts; unresolved-Strike count; action identity; Battle-grid rows before/after; face-up/down Plant counts; Support-resource inventory before/after; and Chronicle sequence boundaries. Concrete legal action variants are counted separately, so Support choices that encode a row/die/card target contribute their target alternatives to the count. Nested target choices made *inside* a selected Plant/Round/Wisp effect are not comprehensively enumerated; that remains a documented limitation of the complexity proxy. Task 6 derives action/event counts from those records plus the existing Chronicle rather than adding a second rules engine.

## Comparable observed metrics

`pacing_windows.csv` is profile-independent observed telemetry. It includes meaningful decision windows, actions, passes, empty opportunities, normal and Sunlight-funded Mains, Support count, action-phase dice rolls/rerolls/placements/moves/value changes, Plant activations/facing transitions, Refreshes, resource deltas, and reassessment components. `pacing_battles.csv` adds the opening Draw-and-Roll dice to the total roll/placement counts and records returned Critters during Cleanup.

A legal-choice complexity proxy is deliberately interpretable rather than CPU-derived:

`min(10, log2(1 + legalChoices) + 0.5 * (unresolvedStrikes - 1))`

Reassessment compares the Battle Grid at an actor's previous opportunity with the Grid when that actor next receives an opportunity. Its capped score is based on changed rows, leader changes, actor-margin changes, up to three dice changes, and up to two Plant-facing changes. This is a proxy for "how much changed while I was waiting," not a psychological measurement.

`pacing_battles.csv` records the observed operation counts plus one modeled duration per timing profile. `pacing_players.csv` records per-player passive waiting, longest uninterrupted passive wait, terminal wait after the player's last meaningful action, consequential opponent-execution watching while a later response remains, and time after the player's last legal opportunity. `pacing_sensitivity.csv` records paired A/B duration and the additional Candidate planning seconds per unresolved-later-Strike unit that would erase the modeled saving. `pacing_summary.csv` provides cohort mean/median/P90/P95 duration, paired duration CI, downtime/tail metrics, action-window/handoff/Support counts, and unused Supports.

Strike resolution, Wounds, Doom and Cleanup are modeled explicitly. Opening Draw is treated as simultaneous; Rank is table-wide; placement is serial. Candidate B resolves each row immediately after its Engagement while A resolves all three after the action loop.

## Human-time assumption profiles

`timing_model.csv` is copied into every experiment output and is the exact assumption set used. The built-in profiles are `fast`, `typical`, and `analysis-heavy`. Parameters include seconds per handoff; base decision time; legal-choice complexity time; later-unresolved-row planning; an explicit Candidate allocation-planning premium; reassessment time; physical execution by Main/Support type; Pass/empty opportunity; Draw/Rank/placement; Strike/Wound/Doom; and Cleanup.

A caller may supply a replacement CSV with `--timing-model PATH`. Its header must exactly match `timing_model.csv`, making sensitivity runs reproducible rather than hidden in code edits. A and B always use the same profile; the only Candidate-specific parameter is the explicitly named allocation-planning premium, which is reported and break-even-tested rather than silently embedded.

## Validation and bias checks

`doc/tools/battle/battle_pacing_task6_report.py` refuses any run containing `FAILURE.txt` or an incomplete cohort. It verifies expected A/B/profile/player row counts, paired opening hashes, contiguous/non-duplicate pacing-window indices, and—critically—cross-checks Task-6 Support totals against the existing Task-5 `summary.csv` totals. A mismatch means the new event instrumentation missed or duplicated actions and the run must not be interpreted.

Filtered resource-rich/asymmetric cohorts retain Task 5's selection semantics: legal openings are classified before either A or B Battle executes. They are conditional stress cohorts, not independent population replications; overlapping base seed schedules mean filtered conditions must not be pooled as though statistically independent.

## Commands

Compile once, then use the existing direct-Java launcher architecture. Example acceptance run:

```bash
python3 bin/a03_verify_boundaries.py
./gradlew --offline compileSimulationKotlin
./gradlew --offline simulationTest --tests '*BattlePacingTimingModelTest' --tests '*BattlePacingABBatchTest' --tests '*ThreeEngagementBattleTest'
bash doc/battle/run_battle_pacing_ab.sh --samples 10 --players 2,3,4 --battles 1,2,3 --policy human-guided --scenario natural --out output/experiments/battle-pacing/task6-natural-10
python3 doc/tools/battle/battle_pacing_task6_report.py output/experiments/battle-pacing/task6-natural-10
```

For the main natural evidence use a new directory and `--samples 100`. For the final-Battle stress checks use separate four-player Battle-3 runs with `--scenario resource-rich` and `--scenario asymmetric`; never merge their estimates into the natural cohort.

## Interpretation boundary and physical calibration

The model can answer "under these explicit action/decision timing assumptions, how much shorter is B predicted to be?" It cannot answer "humans are 20% faster" without physical timing data. A useful calibration playtest should timestamp at least decision-window start, action commitment, physical execution end, each Strike resolution, and Battle end. Those observations can fit or replace the profile parameters while leaving the simulator-observed A/B event sequences unchanged.

Task 7 should reuse these timestamps and decision-window identities but address fairness separately: actual last meaningful actor by Strike, starting row strength, current lead/margin, resources/Plant strength, win conversion, VP/Wound consequences, and controlled order counterfactuals where feasible. Task 6 does not make a causal last-mover claim.

## Locally executed Task 6 acceptance — final patch

Final protected-boundary check: **PASS** after explicitly reviewing and accepting only the two production scheduler files used for the opt-in read-only telemetry seam. `compileSimulationKotlin`: **PASS**. Focused `simulationTest` for `BattlePacingTimingModelTest`, `BattlePacingABBatchTest`, and `ThreeEngagementBattleTest`: **PASS**. The deterministic canonical-A test showed instrumented and non-instrumented copies produce identical actions, Strike outcomes, Doom and VP. The output validator found no missing/duplicate windows and exactly matched the pre-existing Task-5 Support counters.

Final Human-guided simulator runs used 100 paired Battles per cohort (900 natural pairs total), plus 100 resource-rich and 100 asymmetric four-player final-Battle pairs. The natural typical-assumption duration estimates were:

| Cohort | A mean | B mean | modeled savings |
|---|---:|---:|---:|
| 2p Battle 1 | 9.0 min | 5.9 min | 32.0% |
| 2p Battle 2 | 7.0 min | 5.4 min | 19.0% |
| 2p Battle 3 | 6.7 min | 5.3 min | 16.8% |
| 3p Battle 1 | 13.8 min | 8.8 min | 34.6% |
| 3p Battle 2 | 11.4 min | 8.3 min | 25.3% |
| 3p Battle 3 | 10.7 min | 8.2 min | 21.3% |
| 4p Battle 1 | 18.8 min | 11.8 min | 35.5% |
| 4p Battle 2 | 15.0 min | 11.1 min | 24.5% |
| 4p Battle 3 | 13.4 min | 10.4 min | 19.8% |

For the natural four-player final Battle under typical assumptions, A mean/median/P90/P95 were 13.36/13.33/17.10/18.43 minutes; B were 10.43/10.45/11.52/11.65. Mean modeled savings were 176.0 seconds; the paired B-minus-A duration CI was -200.0 to -152.1 seconds. These confidence limits describe simulator-state variation **conditional on the timing model**; they are not a confidence interval on human minutes.

Mean passive waiting per player dropped from 9.09 to 6.90 modeled minutes. Mean terminal waiting after the player's last meaningful action dropped from 2.97 to 2.06 minutes; P95 terminal wait dropped from 6.80 to 4.55 minutes. P95 longest uninterrupted passive wait dropped from 5.93 to 3.14 minutes. Decision windows fell from 28.46 to 11.66 per Battle and handoffs from 25.92 to 10.55. Supports fell from 20.46 to 17.79 per Battle, matching the Task-5 count reduction exactly.

One useful engagement signal moved the opposite direction: modeled time watching **consequential opponent execution while still retaining a later response** rose slightly, from 67.5 to 72.4 seconds/player, while total passive waiting fell sharply. That is consistent with Candidate B concentrating more of the remaining wait into tactically relevant observation rather than simply deleting interaction, but this is still a proxy requiring human validation.

Candidate B also stranded more Support inventory in the natural four-player final Battle: 2.15 versus 2.67 resource units/player. Resource-rich was 2.49 versus 3.18; asymmetric was 2.41 versus 3.06. That is a real simulator-derived tradeoff to investigate in Task 7 rather than hiding it as a pacing success.

The four-player final-Battle result remained directionally similar in both targeted stress cohorts. Resource-rich typical estimates were 14.0 versus 10.7 minutes (22.1% modeled savings); asymmetric were 13.8 versus 10.6 (21.6%).

All three built-in timing profiles made B faster in the natural four-player final Battle: fast 6.5→5.2 min, typical 13.4→10.4, analysis-heavy 21.9→17.0. The model's explicit Candidate-specific allocation premium is 0.4/0.9/1.8 seconds per unresolved-later-Strike unit in those profiles. The additional premium required to erase the observed savings was 2.62/5.70/9.36 seconds/unit respectively. Thus the approximate **total** Candidate-specific premium at equality is 3.02 seconds/unit (fast), 6.60 (typical), or 11.16 (analysis-heavy); larger values make B slower under otherwise identical assumptions.

Balance remains unresolved. The same final runs reproduce Task 5's VP signals: natural 4p Battle 3 B-A Battle VP/player was -0.0725 with 95% CI [-0.2191,+0.0741], but 3p Battles 1/2 and 4p Battles 1/2 had negative CIs excluding zero. Task 6 does not explain those stage-dependent differences and does not treat them as a pacing result. They increase the importance of Task 7's conditioned last-actor/order, early-Wound cascade, resource-stranding and counterfactual fairness analysis before adopting Candidate B.
