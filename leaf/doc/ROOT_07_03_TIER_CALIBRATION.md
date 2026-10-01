# Root_07_03 Effect-Tier Calibration

This experiment estimates which existing fixed Plant cost tier best fits the effect/scoring package currently carried by `Root_07_03` (Root Down Payment).

Canonical card CSVs are not changed. Every experimental file also keeps the known V7 outliers `Vine_07_01` and `Vine_07_04` unavailable.

## Phase 1 — screening

Run:

```bash
bin/experiment_root_07_03_tiers screen
```

Screening tests tiers `7 / 9 / 11 / 14` with three independent learners per tier. The same learner-index training seed cohorts and the same Grove schedule are reused across tiers so tier comparisons are deliberately paired. Each trained policy is then evaluated on the same held-out random-Grove cohort.

The screening defaults are intentionally smaller than the previous full learning runs:

- 8 generations
- population 10
- 50 games per candidate
- 3 learners per tier
- 300 held-out matched samples per learner

The command is resumable. A completed training/evaluation step is marked with a `.done` file and will be skipped on a rerun unless `RESUME=0` is supplied.

At completion the script creates:

```text
output/experiments/root-07-03-tier-calibration/root-07-03-screen-results.tar.gz
```

Send that archive back to ChatGPT before running confirmation.

## Phase 2 — confirmation

After screening analysis identifies the one or two plausible tiers, run confirmation only for those tiers. Example:

```bash
CONFIRM_TIERS=9,11 bin/experiment_root_07_03_tiers confirm
```

Confirmation defaults to:

- 20 generations
- population 16
- 100 games per candidate
- 5 independent learners per selected tier
- 1,000 held-out matched samples per learner

Confirmation uses fresh training seeds, a fresh training Grove schedule, and a fresh held-out evaluation cohort so selection of the candidate tiers from screening does not contaminate the confirmation evidence.

At completion the script creates:

```text
output/experiments/root-07-03-tier-calibration/root-07-03-confirm-results.tar.gz
```

Send that archive back for final tier assessment.

## Override files

The experiment uses:

```text
data/research/plant-overrides/root-07-03-cost-7.csv
data/research/plant-overrides/root-07-03-cost-9.csv
data/research/plant-overrides/root-07-03-cost-11.csv
data/research/plant-overrides/root-07-03-cost-14.csv
```

Each file changes only the effective acquisition cost of `Root_07_03`, while excluding `Vine_07_01` and `Vine_07_04`.

## Reporting note

`Root_07_03` is added to the evaluator's watched-card report. This is research reporting only; it does not change game behavior. The evaluator will therefore print the exact number of held-out samples in which `Root_07_03` was present, final-copy totals, attributed Plant VP, and its Grove-conditional learned/control win share. Combined with the existing `Individual Plant acquisitions` report, this lets the analysis calculate purchases per Grove exposure and observe repeat-copy pressure much more accurately than raw purchase counts alone.
