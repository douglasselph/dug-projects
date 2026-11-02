# 5B.12 approval candidate and resumable refresh batch

## Report review

The uploaded 12-game 5B.12A corrected report shows **816** recorded `Main(...)` or `FinalMain(...)` choices: **545 Plant (66.79%)**, **190 Round (23.28%)**, **81 Draw (9.93%)**, and **zero unrecognized Main labels**. This is a scored-decision sample, not an exhaustive executed-action census; ambiguous bare Battle First Main labels remain excluded. Do not infer card balance from these statistics alone.

## Suggested commands

From repository root, after applying changes and committing them:

```bash
bin/human_baseline_freeze_candidate
bin/human_baseline_refresh_batch baseline
```

To leave it running in a terminal which survives logout:

```bash
nohup bin/human_baseline_refresh_batch baseline > human-refresh-launch.log 2>&1 < /dev/null &
echo $!
```

Resume the same command after interruption; completed steps are skipped. Defaults are 48 decision-diagnostic games, 240 four-player baseline calibration games, and 120 two-player baseline calibration games. Override with `DECISION_GAMES`, `BASELINE_4P_GAMES`, and `BASELINE_2P_GAMES` **before the first run**, not on resume. Each stage has a `.complete` marker only after exit code zero. A source/weight hash mismatch blocks resumed work.

To train expert candidates *after* deciding on this baseline and reviewing the long-run outcome:

```bash
ENABLE_EXPERT=1 bin/human_baseline_refresh_batch expert
```

Expert stage delegates to the existing `bin/train_frozen_policy_group`, with its own resume logic and held-out samples. It **does not** automatically promote weights or rewrite `resync-current.csv`. Existing policies are inputs until explicitly promoted; retraining all groups at once does not automatically create a jointly recalibrated expert. Review each group before promotion, particularly Battle Main depending on frozen upstream groups.

## Scope caveats

- `runBaselineCalibration` uses `FirstGameDefault` Grove and is **not** a full market/card balance report using `resync-current.csv`. Card-specific baseline balance and resync expert card-tier studies require separate runners and should be scheduled after validating the policy checkpoint.
- The batch does not silently execute P1–P4 or S1/S2 research variants.
- Git approval tag is **not created automatically**. Human approval and tests remain explicit.
- Do not change source, training policies, the resync CSV or job parameters during an active/resumable cohort.
- Audit output and individual `.log` files under `output/experiments/human-refresh/<fingerprinted-id>/`.
