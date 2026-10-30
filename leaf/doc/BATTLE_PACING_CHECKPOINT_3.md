# Battle pacing — Checkpoint 3: paired Battle statistics (research-only)

**Source:** October 30, 2026 `leaf(20261030-200906).tgz`; extends Checkpoints 1–2. Canonical v37.03 game, data and scheduling are unchanged. Detailed C04 calibration is not reopened.

## Purpose and interpretation

Build **reproducible paired observations** of an isolated intervention in Battle 1, 2 or 3, under the ordinary 3/2/2 Round Deck, in separate cohorts of 2/3/4 players. Both arms replay **all preceding rounds under canonical A** with the same explicit mechanical and strategy seeds and exactly the same preselected Wisp deck without Resolve / Last Word. Each Battle opening is checked against the copied state fingerprint before either arm acts. A runs normal Step 4/5; B runs the Candidate B row-by-row scheduler from Checkpoint 2. The engine resolves actual VP, Wounds, Doom and Cleanup.

This does **not** evaluate an entire game adopting B for all Battles. Nor does it prove that future RNG streams are always matched once actions diverge. The purpose is to isolate the *local* Battle rules change while preserving the legal state distribution at the selected Battle.

## Added files and small research change

- `.../battlepacing/ThreeEngagementBattle.kt`: records each player's actual Engagement opportunity even when they do not act, the initial row totals, totals before/after the opportunity, legal option counts, and their actual Main/Support choices. No scheduler rule changed.
- `.../battlepacing/BattlePacingStatistics.kt`: pure online paired-sample stats, aggregate action counts, last-actor statistics, initial-row and immediately-prior-to-turn winner projections (with canonical all-tied rule), confidence intervals.
- `.../battlepacing/HumanGuidedEngagementPolicy.kt`: **exploratory** Candidate B adapter using the existing Human Main/Step-5 choices, Human B11/B12 Support scorer and Wisp hold/play decision layer; may take zero/one normal Main and up to two Supports per opportunity. *It is not certified as equivalently skilled in the different B action economy.*
- `.../battlepacing/BattlePacingABMain.kt`: complete seed-controlled CSV batch runner; one cohort per player count / Battle ordinal / policy.
- `doc/run_battle_pacing_ab.sh`: launcher via existing `writeSimulationRuntimeClasspath` Gradle task, **without** changing protected `build.gradle.kts` or bin launchers.
- `src/simulationTest/.../battlepacing/BattlePacingStatisticsTest.kt`: pure stats edge cases.
- `src/simulationTest/.../battlepacing/BattlePacingABBatchTest.kt`: real A/A deterministic outcome replay and single mechanical A/B batch file invariants.

## Important gates and limitations

1. **Mechanical correctness precedes balance.** Run Kotlin regressions and the `--policy mechanical` 1, then 100/1,000 samples. Errors in legal-target narrowing, locked-row effects, Wispquake, or card interactions **abort** the cohort. A failing `FAILURE.txt` is **not** a trial or statistical observation. Checkpoint 2's known specialized-effect audit is not automatically complete merely because a batch finishes.
2. **Policy parity remains a material limitation.** Canonical A uses its own Mechanical or Human policy. Candidate B uses its Mechanical or Human-guided adapter, which necessarily differs in Pass/one-window/Support valuation. The Human-guided condition is *credible tactical exploration*, not proof of a causal B rule advantage. The adapter uses the existing Human baseline analysis and cards, not a fresh C04 calibration.
3. **Effect handling is not comprehensively certified.** In particular, swaps, mandatory cross-row targets, multi-draw chains and locked Critter modifiers must pass dedicated edge-case tests. An arbitrary run without encountering the effect is not proof of its legality. Wisp Resolve is **excluded identically** from both arms; Wispquake and all dice effects are subject to the absolute locked-row rule.
4. **No post-hoc dropping of failures.** A batch fails immediately on first exception and writes the seed / cohort / stack trace to `FAILURE.txt`. Re-run the exact scenario after fixing the fidelity issue; never remove the failing samples and interpret the remaining subset as representative.
5. **Statistics.** The primary paired difference is `CandidateBattleVP - ControlBattleVP` *per player averaged within a Battle*. This yields one paired observation per Battle, not artificially independent per-player observations. Also output Strike VP delta and Wound delta. 95% paired-mean intervals use a normal approximation and require more than one pair. Last-actor unique-win rate uses Battle-clustered mean intervals (three correlated rows per Battle). Its raw rate/swing is **not** an adjusted positional advantage; initial die strengths, seating and intentional future-row steering confound raw comparisons. Use the per-row rows + controlled seat/order-swap study before causal claims.
6. **No observed human minutes.** Action counts and empty opportunities are directly simulated; they are not measured wall-clock playing times. An uncalibrated human-time model belongs to Checkpoint 4.
7. **Coverage.** This first runner samples ordinary legal game prefixes and records each player's starting/remaining Support inventory; it does not force every heavy-resource final-Battle or 1-versus-12 asymmetric scenario. For acceptance, explicitly add and validate those targeted strata, plus stronger tactical-policy parity and A/B order-swap counterfactuals. Do not infer representative resource-heavy results from a sparse tail.
8. **Catalogs.** Uses the current `data/` canonical catalogs, selected Grove generated by GameFactory, no resync overlays or custom weights. Record any desired experimental Plant/Round overrides as a separate comparison factor, never silently mix them into these cohorts.

## Installation and verification (ZIP placed in ~/Downloads)

```bash
unzip -o ~/Downloads/leaf_battle_pacing_checkpoint3.zip -d ~/src/dug-projects/leaf
cd ~/src/dug-projects/leaf

python3 bin/a03_verify_boundaries.py
./gradlew compileSimulationKotlin
./gradlew simulationTest \
  --tests '*EngagementOrderSelectorTest' \
  --tests '*PairedBattleOpeningReplayTest' \
  --tests '*ThreeEngagementBattleTest' \
  --tests '*BattlePacingStatisticsTest' \
  --tests '*BattlePacingABBatchTest'
```

### Run Mechanical Control first

```bash
# 1 seed per cohort: 9 total paired Battles; validates the runner/output interface.
bash doc/run_battle_pacing_ab.sh \
  --samples 1 --players 2,3,4 --battles 1,2,3 --policy mechanical \
  --out output/experiments/battle-pacing/mechanical-1

# 100 seeds per cohort: small correctness pilot, no reliable balance conclusion.
bash doc/run_battle_pacing_ab.sh \
  --samples 100 --players 2,3,4 --battles 1,2,3 --policy mechanical \
  --out output/experiments/battle-pacing/mechanical-100

# If all focused legality audits pass, larger mechanical sanity batch.
bash doc/run_battle_pacing_ab.sh \
  --samples 1000 --players 2,3,4 --battles 1,2,3 --policy mechanical \
  --out output/experiments/battle-pacing/mechanical-1000
```

### Then exploratory tactical A/B

```bash
# Tactical smoke. HUMAN A vs human-guided B; labels make policy mismatch explicit.
bash doc/run_battle_pacing_ab.sh \
  --samples 10 --players 2,3,4 --battles 1,2,3 --policy human-guided \
  --out output/experiments/battle-pacing/human-guided-10

# ONLY if 10-sample tactical smoke and fidelity audits pass:
bash doc/run_battle_pacing_ab.sh \
  --samples 1000 --players 2,3,4 --battles 1,2,3 --policy human-guided \
  --out output/experiments/battle-pacing/human-guided-1000
```

For controlled replication, supply e.g. `--seed 2026103000 --strategy-seed 2026104000 --wisp-seed 2026105000`. **Output directories must not already exist**: the runner refuses to mix previous experiments with new seeds/results. A run with `FAILURE.txt` is invalid and must not be used as statistical evidence.

## Files produced in each output directory

- `manifest.txt`: research settings, seeds, Wisp exclusion, per-cohort sample count, data CSV hashes when present, policy caveats.
- `pairs.csv`: exact seed/fingerprint, individual player's pre-Battle VP and gains under both rulesets, beginning and remaining Support resource inventories, and pure FinalScorer projections (actual final scores/winners ONLY for Battle 3; for Battles 1/2 these are strictly “if the game ended now” snapshots).
- `strikes.csv`: actual A/B winners, Wounds and VP, B row starter and last actor, starting total/initial projected winners, totals immediately before/after the last actor, last-actor unique/shared wins and tactical swings.
- `opportunities.csv`: all B actor windows, including empty/pass windows, actor position, chosen Main/Supports, legal action menu sizes, before/after row totals.
- `summary.csv`: per-cohort sample count; average paired Battle VP, Strike VP and Wound changes; 95% intervals for Battle VP and B last-actor unique-win share; A/B normal Main/Support counts; B zero-action windows and last-actor swings; CPU seconds.
- `FAILURE.txt` if ANY sample fails; reject the entire incomplete run.

## Next acceptance gate (before presenting balance findings)

1. Receive the user's **actual** successful Kotlin test and pilot-run logs; fix any compile or execution errors and stop if the mechanical-fidelity gate fails.
2. Add a targeted state inventory for B's locked-row mandatory swaps, multi-draw effects, Wispquake and immediate Wounds; assert no unsupported states escape into summary CSV.
3. Measure coverage of high-resource and asymmetric final Battles, then construct deterministic legal targeted cases if underrepresented.
4. Confirm B tactical policy suitability (and, ideally, run alternate tactical and bounded-search policies for sensitivity), then run ≥10,000 valid paired Battles per key cohort when practical. Report adjusted positional effects separately from the raw last-actor wins.

## Local verification in the assistant environment

- `python3 bin/a03_verify_boundaries.py`: PASS, 480 reference/current protected files, zero protected changes.
- `python3 -m unittest discover -s bin/tests -p test_a03_boundaries.py -q`: 19 passed.
- `BattlePacingStatistics.kt`: isolated Kotlin compilation with local API stubs PASS. This is **not** compilation of the real research source set.
- `./gradlew compileSimulationKotlin --offline`: blocked **before Kotlin compilation** by missing cached Compose/Koin/Kotlin JARs. No real Kotlin regression, complete Battle or statistical pilot could run locally. No A/B empirical values are claimed.
