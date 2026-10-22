# Task 5B.12 — Human Baseline calibration and approval checkpoint

## Decision

**Candidate; not approved/frozen.** This checkpoint is deliberately fail-closed. No scoring weights or gameplay behavior are changed, and no Task 6 expert comparisons should be treated as approved-baseline comparisons until the owner reviews and records approval.

## Scope and evidence

Source archive: `leaf(20261022-224449).tgz`.

A new reproducible runner `bin/human_baseline_5b12_checkpoint` records source/data/build SHA-256 manifests and Git revision/dirty state, all core test suites, targeted decision regressions, two distinct fixed-seed four-player baseline-only development diagnostics with a replay of cohort A, and two-/four-player baseline-only cumulative seat/VP calibrations. Failure of any Gradle step stops the process; identical cohort output is required for repeated seeds.

Run from the repository root:

```bash
bin/human_baseline_5b12_checkpoint
# Or pass an output directory:
bin/human_baseline_5b12_checkpoint "$HOME/leaf-baseline-review/5b12"
```

Gradle tasks already in the repository:

- `runBaselineRandomnessDiagnostic`: fixed-seed per-game development and winner diagnostics.
- `runBaselineCalibration`: seat win share, average VP, tie rate, and neutral-seat sampling-reference context, for cumulative checkpoints.
- `test integrationTest simulationTest`: rule and strategy regressions.

The runner uses `FirstGameDefault.PLANT_NAMES` consistently across cohorts; **it is a calibration screen, not a comprehensive market/environment test**. To use other card sets, invoke the existing runners separately with `-Pplants=<comma-separated-names>` and archive those results against the same source hashes.

## Actual environment result in assistant sandbox

The Gradle 8.13 distribution supplied in `leaf-gradle-portable.tar.gz` started, but compilation/simulations did not complete. The attempt stopped/was timed out immediately after `checkKotlinGradlePluginConfigurationErrors` (log available separately). **Zero controlled games have been verified here. No win-share, VP, or action-frequency estimates are claimed.**

## Score-explanation and choice-distribution coverage: BLOCKER

`BaselineScoreEngine` can emit typed `DecisionReasoning` via `DecisionReasoningSink`, including all scored alternatives, score breakdowns, selection flags, and observations. Normal large simulation strategies explicitly pass `DecisionReasoningSink.NONE` for overhead reasons. Existing `BaselineResearchMain` renders game/winner/development and seat-level aggregate outputs **but does not report action type frequencies, chosen-vs-rejected score distributions, or reason frequencies**. Therefore neither this script nor those reports proves Draw-versus-Plant-versus-Round calibration. Full approval requires capturing those traces in a controlled sample or adding an instrumented game runner that passes a recording sink to each Human Baseline player and aggregates per decision category.

At minimum manually inspect sampled `DecisionReasoning` snapshots from controlled constructed decision tests for:

- Cultivation Draw / Plant / Round effect comparability, including no duplicated opportunity-cost penalty;
- Buy Plant acquisition usability vs immediate activation (distinct priorities);
- Battle Main activation vs target score for Sapping S1/S2, Petal P1-P4, collateral effects;
- O Edelweiss first/second nested choice, and 0/1/2-step stop;
- Bee/Butterfly theft/refresh and opponent denial without double counting;
- tie rates and unexpected fixed bonuses dominating projected state consequences.

## Approval checklist

- [ ] Tests `test`, `integrationTest`, `simulationTest` pass.
- [ ] Choose-then-execute regressions pass (including Petal cycling and O Edelweiss intermediate state).
- [ ] Two replays at identical seeds match, with a different-seed cohort showing appropriate variation.
- [ ] 2- and 4-player calibration distributions reviewed for positional bias and VP/development pathologies (sampling uncertainty acknowledged).
- [ ] Decision-type frequencies and score explanation distributions captured/reviewed (not currently exposed by game-level runner).
- [ ] No unreviewed score double counting or choice/target disagreement in sampled states.
- [ ] Exact source hash, data hash, seed cohorts, relevant rule version, and caveats recorded in approved commit/tag.

### Proposed versioning convention

Once all conditions are met, tag the corresponding *clean* Git revision, e.g. `human-baseline-v35-5b12-approved-1`, and include this report plus generated evidence in the review record. Do not tag the current snapshot approved automatically.

## Next step before Task 6

Wire a diagnostic-only `DecisionReasoningSink` through a small baseline-only runner and record action-category histograms, winner-vs-alternative score margin distributions, and adjustment reason frequencies. Keep collection off by default for experiments. Re-run the same deterministic seed cohorts after any scoring change; changes to the baseline require a new hash and explicit reapproval.
