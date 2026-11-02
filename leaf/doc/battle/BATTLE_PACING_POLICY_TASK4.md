# Battle pacing — Task 4: Candidate B rules-fidelity audit and repair

Source snapshot: `leaf(20261101-025733).tgz`, after Tasks 1–3. The source archive is the implementation baseline; v37.03 (especially pp. 14–18, 20–24) and the agreed experimental row-lock rule are the rules references.

## Supplied Task 3 pilot — what it does, and does not, establish

The supplied `policy-task3-human-final-10/summary.csv` contains **10 completed paired 4-player final Battles**. Both arms performed 80 normal Main Actions. Baseline A performed **199** Supports; Candidate B performed **180**. Candidate B had **13 no-action opportunities out of 120**, 29 recorded last actors who actually acted (of 30 Strikes), and 5 last-actor swing events. Candidate B minus A Battle VP per player was essentially zero, with reported CI95 approximately `[-0.5165,+0.5165]`. The ten-pair sample is too small for balance or timing conclusions. JVM CPU seconds cannot stand in for tabletop minutes.

## Reviewed fidelity fix in this patch

`src/main/.../effect/special/PetalToDie4Effect.kt` previously offered *every* opponent Hand D4 as a steal source during Battle, including D4s physically retained in already resolved Strikes. Trying to remove such a D4 could throw **after** a Main Action was committed, and violating a locked row would be illegal. The printed/historical Petal variant also applied +4 to every surviving Hand die, including dice in a locked Strike.

- Battle steal-source generation now includes only opponent Hand D4s **placed in open Strikes**, while preserving original Hand indices for the decision API. Discard-zone theft (where the research variant permits it) and Cultivation remain unchanged.
- Battle execution revalidates the chosen Hand die's identity/location and open-row status immediately before removing its Grid location.
- The old printed variant still Trashes its selected open-row D4 through the common `TrashResolver`, but only Raises remaining dice in **open** rows. Cultivation retains the original all-Hand behavior.
- Added deterministic effect tests: only locked opponent D4 (not legal), open D4 beside locked D4 (legal, exact original index), printed +4 with locked first Hand die (unchanged), and Wispquake with locked dice owned by *both* players.
- The production v37 path is unchanged when all three rows are open; there are no modifications to default Battle scheduling, evaluation policies, source data, or A/B seeds.

This is a narrow correctness change, **not** evidence that all possible effect chains are certified.

## Existing source and test coverage inspected

| Rule / interaction | Observed implementation and deterministic coverage | Gate |
|---|---|---|
| Candidate B one turn opportunity per player per High/Middle/Low | `ThreeEngagementBattle` checks one Main and two Supports per Engagement; `ThreeEngagementBattleTest` covers legality | Existing tests; rerun |
| Main tokens: two total, with optional deferral | `normalUsed`, Task 1/2 timing planner; tests exercise forced middle/low | Existing tests; rerun |
| Sunlight counts as Support and funds a token-free Main | `BattleActionCoordinator.researchTakeSupport` reuses canonical executor | Targeted chained-effects test still desirable |
| Immediate VP/Wounds and Flip-or-Snip | Production `StrikeResolver.resolveRow` and `WoundResolver` are called before row lock; existing unit tests for each | Targeted mid-Battle Wound + later Plant action test still desirable |
| Immovable locked dice/Critters, row total freezing | `BattleGrid`, `Die`, `BattleState.lockResolvedRow`, `LockedStrikeEvidence`, unit/simulation tests | Existing tests; rerun |
| Pollen Theft, Root Well | `battleDieChoices` filters globally closed rows and validates concrete targets | Add full end-to-end lock tests before high-volume A/B |
| Wispquake | Effect explicitly skips closed rows for keep and rerolls; new two-player locked-row regression | New test; run |
| Petal To Die 4 | This patch filters locked-row theft and raises; new regressions | New tests; run |
| Root on a Roll / Root Recall / Bursting Blossom / Vine and Dine | Previously repaired open-row targeting; tests already present | Existing tests; rerun |
| D4 Trash, Doom and Cleanup | Shared `TrashResolver`, production `DoomResolver` and Cleanup; post-Battle removal phase after all rows resolve | Existing tests; rerun |
| Wisp Resolve / Last Word | `BattlePacingWispDeck.withoutResolve` excludes it **from both A and B** before replay | Existing deck test; rerun |
| Scarce shared resources and Wisp recursion | Production effect executors reused, but interactions can change legal actions mid-turn | Not exhaustively certified |
| Multi-Strike Plant effects and mandatory swaps | Open-row targeting is largely centralized, but combinations/corner cases need targeted tests | Not exhaustively certified |

## Policy-fidelity distinction

Human-guided Candidate B reuses existing production Main and Support scorers; the Task 2 Main-timing forecast is still an approximation, and Task 3's B12 enabling paths can undercount a future deferred Main. Those are **policy parity limitations**, not new rules permission. They must not be erased by mechanical-fidelity tests. Do not reopen broad C04 calibration.

## Acceptance in Ubuntu checkout

From the leaf top level, after unzipping the patch:

```bash
python3 bin/a03_verify_boundaries.py
python3 -m unittest discover -s bin/tests -p 'test_a03_boundaries.py' -v
./gradlew test --tests '*PetalToDie4EffectTest' --tests '*WispquakeEffectTest' --tests '*VineAndDineEffectTest' --tests '*DrawEffectHandlerTest' --tests '*DoomResolverTest' --tests '*BattleGridTest' --tests '*WoundResolverTest'
./gradlew simulationTest --tests '*EngagementSupportContinuationTest' --tests '*ThreeEngagementBattleTest' --tests '*BattlePacingABBatchTest'
```

Only after tests pass, run a compact real-engine regression across player counts and Battles (with a *new* output directory):

```bash
bash doc/battle/run_battle_pacing_ab.sh --samples 10 --players 2,3,4 --battles 1,2,3 \
  --policy human-guided --out output/experiments/battle-pacing/task4-human-all9-10
```

The existing launcher uses Gradle **once** for classpath compilation and launches a **persistent direct-Java JVM** for the batch; it archives results (including partial results on failure) in `~/Downloads`. Check the report for exactly 90 completed pairs, zero `FAILURE.txt`, all B Engagements capped at one Main/two Supports, and that important Supports actually occur. If a single exception occurs, stop and diagnose the exact reproducible sample; do **not** omit the sample and recalculate aggregate statistics.

## Verification results — executed against the uploaded source snapshot

- The A03 manifest was updated for **one explicitly reviewed protected handler change**. A03 boundary verification and all **19** Python boundary tests passed locally.
- The focused `PetalToDie4EffectTest` and `WispquakeEffectTest` **passed** with Gradle 8.13 and the supplied portable offline cache (the new Kotlin test cases executed).
- The broader existing `VineAndDineEffectTest`, `DrawEffectHandlerTest`, `DoomResolverTest`, `BattleGridTest`, and `WoundResolverTest` **passed**.
- `simulationTest` for `EngagementSupportContinuationTest`, `ThreeEngagementBattleTest`, and `BattlePacingABBatchTest` **passed**.
- A 10-sample **Human-guided**, 2/3/4-player, Battles 1/2/3 smoke run **completed all 90 paired Battles** without errors. It used a single direct-Java JVM after one Gradle compilation, matching the latest launch architecture. Results in `task4-human-all9-10/` and a compressed archive (a copy is available with the handoff).
- As expected for tactical games, both arms spent Supports in every cohort; e.g. 4-player final Battle used **199** A Supports and **180** B Supports across 10 pairs.

**These are verified tests and simulated outcomes, not proof of exhaustive rules fidelity or real-world shorter playing time.** The audit's untested interaction classes remain listed above and should receive additional targeted tests if high-volume trials surface edge cases. Do not pool the small pilot's VP figures across policy revisions or extrapolate ten samples into a balance conclusion.

## Next: Task 5

After accepting Task 4, run credible, seeded Human-guided A/B cohorts for 2/3/4 players and Battles 1/2/3, deliberately supplementing naturally encountered Battles with high-Support and asymmetric-resource final Battle states. Preserve identical pre-Battle openings within each pair. Track Main allocations, Support utilization, locked-row effect incidents, Wounds and VP, and do not pool older policy versions. *Then* Task 6 instruments matched decision-time proxies and passive downtime in A and B, followed by Task 7 last-actor and snowball analysis.
