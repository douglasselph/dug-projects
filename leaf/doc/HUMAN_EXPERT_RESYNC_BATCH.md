# Current-resync refresh launcher

Command: `bin/human_expert_resync_batch [preflight|baseline|expert-buy|expert-groups|all]`.

This script is a **coordinator of existing workflows**, not a new balance simulator. It never modifies `data/research/resync`, overwrites canonical `data/ai` weights, or promotes trained policies. All generated data is written under `output/experiments/human-expert-resync/<commit>-plant-<hash>-round-<hash>/` unless `OUTPUT_ROOT` overrides it.

## Suggested order

1. Commit the launcher, then `bin/human_expert_resync_batch preflight`.
2. If you need to leave soon, run `nohup bin/human_expert_resync_batch expert-buy > resync-refresh-launch.log 2>&1 < /dev/null &` and save `$!`. This can be **expensive**: by default five independent learned Buy policies for each of 2, 3 and 4 players, with 2,500 matched evaluation samples per learner. Use `LEARNERS=2 EVAL_GAMES=250 GENERATIONS=4 POPULATION=8 TRAIN_GAMES=30` for a short shakedown.
3. Baseline-only diagnostics may be rerun with `bin/human_expert_resync_batch baseline`. These use the **default selected Grove**, not the entire resync market.
4. Run `bin/human_expert_resync_batch expert-groups` only after reviewing the training environment and the existing frozen-policy weights. Each group trains and evaluates candidates, but subsequent groups continue using the checked-in frozen weights. **This launcher does not stage newly trained group winners into subsequent training**. Manual promotion and a new frozen revision are required for true sequential recalibration.
5. For a full unattended sequence of the *existing* stages, use `bin/human_expert_resync_batch all`, with those caveats.

## Critical scope distinctions

- **Baseline**: 48-game decision diagnostics and two/four-player calibration using the simulator's selected default Grove. Not a per-Plant `resync-current.csv` balance report.
- **Expert Buy**: `experiment_current_market_confirmation` performs fresh Buy-only learner training/evaluation on randomized Groves and emits structured per-card summaries, exposure and purchase information, held-out matched control-vs-learned comparisons. The expert is the **Buy** policy; all other decisions retain their existing policies. Reports are in `expert-buy-current-market/reports/`, notably `card-summary.tsv`, `slot-summary.tsv`, `zero-purchase-cards.tsv`, and `README-FIRST.txt`.
- **Expert groups**: existing `train_frozen_policy_group` pipeline; produces independent candidates by group; no automatic weights promotion or integrated all-expert rerun.
- **Still needed** for the user's ultimate goal: an all-card, pure-Human-Baseline balance telemetry run explicitly against the resync market, then refreshed **joint** expert configurations and a verified comparison of card balance. Neither is falsely claimed by this launcher.
- **Not included**: Petal P1-P4 / Sapping S1-S2 alternative effect studies.

## Reproducibility and resume

Preflight validates `resync-current.csv` as an exact four-card-per-slot market. It records Git commit, SHA-256 hashes of resync files, source code, research scripts and weight files, plus game/training settings in `config.lock`. A previous run with different inputs is refused. Successful stages get `*.complete` markers; failed stages keep logs and can be restarted. The underlying market runner and training scripts additionally implement learner-level resume. The launcher does not delete old results.

`--help` prints all supported environment inputs. Do not change seeds/settings or tracked sources midway through a batch. Output directory can be varied with `OUTPUT_ROOT`. The launcher requires committed tracked work by default; `REQUIRE_CLEAN_GIT=0` bypasses this guard but not the locked input fingerprint.

## Examples

```bash
bin/human_expert_resync_batch preflight

LEARNERS=2 EVAL_GAMES=250 GENERATIONS=4 POPULATION=8 TRAIN_GAMES=30 \
  bin/human_expert_resync_batch expert-buy

nohup bin/human_expert_resync_batch expert-buy \
  > resync-refresh-launch.log 2>&1 < /dev/null &
echo $!

# Continue an interrupted run with unchanged inputs:
bin/human_expert_resync_batch expert-buy
```

## Verification performed when authored

- Bash syntax (`bash -n`): PASS.
- Current market exact-4x9 validation: PASS on uploaded source.
- Dry-run stage planning: PASS.
- Long-running Gradle/Python training and evaluation: **not executed here**; run and review outputs locally.
