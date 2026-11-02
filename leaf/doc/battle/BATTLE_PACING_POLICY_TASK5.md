# Battle pacing — Task 5: paired tactical-cohort evidence

Implementation base: `leaf(20261101-031341).tgz`. Read `doc/engineering/SIMULATION_LAUNCH_ARCHITECTURE.md` before changing the launcher; it is unchanged by Task 5.

## What is new

The existing runner now accepts `--scenario natural|resource-rich|asymmetric` and `--max-attempts N`. These classify **unmodified, legal canonical pre-Battle openings** (including Cultivation/Buy behavior). Neither arm's battle has been executed when selection happens. Both A and B retain the same initially drawn dice, round cards, resources, Plant creature, scores, RNG seeds and Wisp deck with Resolve excluded identically.

- `natural` (default): select all openings, preserves old sample seeds and results contracts.
- `resource-rich`: at least one player has **8 or more** available Support *resource units* (Bee, Worm, Water, Sunlight, stored Mulch die, owned Wisp, ready Butterfly). The number of legal actions may be lower; not all units will necessarily be useful.
- `asymmetric`: maximum initial Support inventory **>=8** AND max-minus-min **>=5**. Deliberately stresses uneven resource holdings without fabricating legal states.

Selection rejection merely discards a paired *opening* before execution; it does not drop failures, unfavorable outcomes or any A/B comparisons. An exception during a selected Battle **aborts** the experiment. An inability to fill a requested cohort within `--max-attempts` similarly aborts with `FAILURE.txt`. A filtered cohort measures outcomes conditional on its selection profile, **not** the population-level average. Do not pool filtered and natural estimates.

Seed indices in the old runner were `sample`; now for filtered cohorts seeds use `attemptIndex` while `sample` in the existing CSVs is the accepted index. For `natural`, `attemptIndex == sample` so historical reproducibility is preserved. `opening_selection.csv` records both, all rejected openings, actual seeds, fingerprint and resource/VP heterogeneity. `selection_summary.csv` records acceptance rates and attempts. The old `pairs.csv`, `strikes.csv`, `opportunities.csv`, `main_plans.csv`, `summary.csv` headers and semantics are retained. `manifest.txt` includes scenario, thresholds, attempted seed schedule and relevant hashes.

`doc/tools/battle/battle_pacing_task5_report.py` refuses failed or incomplete cohorts, checks accepted-row/pair counts, then prints side-by-side A/B Supports, mean VP changes and confidence intervals. It does not claim simulation CPU time is tabletop playing time.

## Petal To Die 4 research decision

The only **active** variant in `data/Cultivation_Cards - VF.csv` is:

> Gain or Steal a D4. Discard any number of D4's. Draw a die for each D4 discarded.

The older effect enums/handlers remain for historical compatibility; no other Petal variant was adopted in the current card data. `PETAL_TO_DIE_4_ALLOW_RESOLVED_STRIKE_THEFT` is an `internal const val` in `PetalToDie4Effect.kt`, currently **false**. With false, open-row-only theft is enforced during Candidate B, preserving full row immutability and Doom. An experimental **true** setting is *not yet safe*: BattleGrid's closed-row physical ownership and Doom would also need an explicitly reviewed resolution policy and a lock-evidence update. Do not toggle it for valid statistical runs until that implementation is supplied. This flag makes the design decision visible without silently permitting mechanically inconsistent theft.

## Acceptance plan

1. First verify A03 and Kotlin tests. Run a ten-pair natural Human-guided pilot for 2/3/4 players and Battles 1/2/3, compare with Task 4 (same seed schedule and behavior; Petal flag OFF).
2. Run 100 Human-guided pairs per 2/3/4 player count, Battle 1/2/3 (`--scenario natural`). Check nine complete cohorts, provenance and error-free action traces. Report 95% paired confidence intervals; do not pool stages indiscriminately.
3. Run targeted **four-player final Battle** `--scenario resource-rich --samples 100` and separately `--scenario asymmetric --samples 100` with *different output directories*. Note acceptance rates and initial resource distribution. `--max-attempts 4000` is a practical upper bound, not a promise all desired cases are common.
4. If early pilots detect a locked-row interaction failure, fix and rerun entire affected cohort with a new output directory; never remove that sample and call the remainder complete. Before extrapolating to 1,000+, inspect Main timing allocation, support counts, residual inventories, VP and Wound outliers, and strategically implausible actions.
5. For strong claims about last-actor advantage, add matched baseline observational metrics and controlled order swaps in Task 7; Task 5 Candidate-B-only `lastUniqueRate` is **not** causal or comparable to baseline as currently logged.

The runner replays the game's canonical **prefix**, then one selected Battle. Earlier Battles' final-score columns mean "if the game ended now"; only Battle 3 is a true final-game score. This is an intervention-at-Battle design, not a whole-game strategy adaptation to Candidate B.

## Verification commands

After unpacking in the project root:

```bash
python3 bin/a03_verify_boundaries.py
./gradlew compileSimulationKotlin
./gradlew simulationTest --tests '*BattlePacingScenarioTest' --tests '*BattlePacingABBatchTest'
bash doc/battle/run_battle_pacing_ab.sh --samples 10 --players 2,3,4 --battles 1,2,3 --policy human-guided --scenario natural --out output/experiments/battle-pacing/task5-natural-10
python3 doc/tools/battle/battle_pacing_task5_report.py output/experiments/battle-pacing/task5-natural-10
```

The launcher compiles once and runs the entire batch in a direct-Java JVM, then archives a copy into `~/Downloads` on completion or failure. Output directories must be new; it refuses overwrites.

## Next tasks

**Task 6:** collect comparable A and B decision windows, handoffs, board reassessments and meaningful-action downtime. Fit transparent low/base/high *model-estimated* player times, with sensitivity thresholds. **Task 7:** control for opening row strength and action order in last-mover analysis for both rulesets and examine immediate-Wound cascades. Neither task's conclusions can be extracted directly from this Task 5 data.

## Locally executed Task 5 acceptance (same source patch)

Portable offline Gradle `compileSimulationKotlin`: **PASS**. `simulationTest --tests '*BattlePacingScenarioTest' --tests '*BattlePacingABBatchTest'`: **PASS**, including real selected-opening replay. A03 guard and all 19 Python boundary checks: **PASS**.

Using one build then one direct-Java invocation per deliberately separate scenario condition, the following Human-guided results **completed with no exceptions**:

| Cohort | Accepted pairs | Legal openings examined | A Supports/player | B Supports/player | Mean B-A Battle VP/player (95% CI) |
|---|---:|---:|---:|---:|---|
| All 9 natural cohorts (2/3/4 players, Battles 1/2/3) | 900 | 900 | see `task5-natural-100/summary.csv` | see `summary.csv` | cohort-specific |
| 4p final, resource-rich | 100 | 164 | 5.50 | 4.65 | -0.0675 [-0.2003,+0.0653] |
| 4p final, asymmetric | 100 | 227 | 5.40 | 4.65 | -0.0700 [-0.2067,+0.0667] |

Natural 4p final: A=5.12 Supports/player; B=4.45; mean B-A VP/player -0.0725, CI [-0.2191,+0.0741]. No human-time estimates exist. Different scenarios intentionally reuse the same base seed schedules, so selected opening populations can **overlap**. Do NOT treat filtered scenarios as independent replications or combine their statistical estimates. All results are simulator-derived under the current Human-guided tactical policies; subsequent policy changes require new separately labeled runs.
