# Resync experiment progress / timing

This is a presentation-only update. It changes neither Human Baseline nor
expert policy scoring, research outcomes, seeds, weights, or resync market.

## Live expert output
The outer launcher previously redirected its entire child process to a stage
log. The child already had per-learner completion estimates and a one-minute
silence heartbeat. The outer launcher now forwards those messages to stdout
using `tee`, preserving the detailed stage log and correct failure status
(`set -o pipefail`). The selected-stage wall-clock duration is printed at end.

**Already-running studies are unchanged**. To monitor one right now, read
`expert_buy_current_market.log`, whose exact path was announced in the first
line of the outer log. `tail -f` it from a second terminal; do not restart.

## Human Baseline card research
The new baseline-only resync card runner uses
`bin/lib/experiment_run_timing.sh` for per-chunk progress, measured mean
runtime, estimated remaining duration, and local completion time. Skipped
chunks never pollute the duration average. `Run X of Y` counts chunks, **not**
individual games. For more frequent ETA updates use a smaller `CHUNK` value
when launching a new cohort, at the cost of additional Gradle startups.

## Run and monitor

```bash
bin/human_expert_resync_batch expert-buy \
  |& tee ~/Downloads/resync-refresh-launch.log

PLAYERS=4 GAMES=2400 CHUNK=50 bin/human_baseline_resync_card_balance \
  |& tee ~/Downloads/human-resync-4p.log
```

The baseline script still redirects each *individual Gradle chunk's* detailed
output to its per-chunk log. The outer log shows chunk starts, completions,
measured durations, and ETAs. No unverified speed estimate is printed before
at least one chunk has completed.

Validation performed on artifact: Bash syntax checks. No research simulations
were executed; compile/runtime behavior of the Kotlin runner is unverified.
