# Pure Human Baseline resync-market card-balance study

## Purpose
Run **only Human Baseline players** through independently sampled legal 9-slot
Groves from `data/research/resync/resync-current.csv`. Use the matching current
Round effect override file. No expert weights, planned acquisition biases,
Petal variants, or Sapping variants are involved.

This study is independent of `bin/human_expert_resync_batch expert-buy` and
writes to a separate output tree. Results characterize *natural baseline*
exposure, legal purchase opportunities, acquisitions, activations, and observed
performance. They **do not** claim causality or prove effect balance from wins.

## Run
From the repository root, after extracting and committing the files:

```bash
PLAYERS=4 GAMES=2400 CHUNK=100 bin/human_baseline_resync_card_balance \
  |& tee ~/Downloads/human-resync-4p.log
```

To resume after interruption, rerun exactly the same command. Each chunk is
written first to `.tmp` then atomically moved to `.tsv` and marked `.complete`.
A process lock prevents two instances of the *same cohort* from running
simultaneously. Reusing the same seeds and settings is deterministic.

You can run player counts separately:

```bash
PLAYERS=2 GAMES=1200 CHUNK=100 bin/human_baseline_resync_card_balance \
  |& tee ~/Downloads/human-resync-2p.log
PLAYERS=3 GAMES=1200 CHUNK=100 bin/human_baseline_resync_card_balance \
  |& tee ~/Downloads/human-resync-3p.log
```

For a quick smoke run, use `GAMES=4 CHUNK=2`. **The first full run should be
performed only after this smoke test and its report have been inspected.**

## Report
`output/experiments/human-resync-card-balance/<run-id>/` contains:

- `manifest.txt`: exact Git commit, Kotlin source hash, resync input hashes,
  player count, game/chunk count, seeds.
- `market-preflight.txt`: validation output for market slot configuration.
- `chunks/sample-...tsv`: one observed row per Plant identity per game;
  only marked-complete chunks are aggregated.
- `chunks/sample-...log`: each Gradle run, including failures.
- `card-balance-summary.tsv`: sums and descriptive exposure-normalized rates.

Metrics: Grove exposure, affordable/legal Buy opportunities, selections,
purchases, Plant activations, owned copies at end, win share among owners,
owner VP and total game VP. Purchases can exceed 1 per exposure (multiple
players/copies). Activation-per-purchase is *a count ratio*, not probability.

**Interpretation:** Raw acquisition counts favor cards that are exposed more.
Use exposure denominators and legal-opportunity denominators; the latter may
still have substantial player-choice and game-state confounding. The owner's
VP and win share are observational and especially susceptible to selection
bias. Do not infer a card's causal contribution from them or overwrite
`resync-current.csv` from this report alone. Expert runs and controlled
variant interventions address other questions.

## Engineering and performance
The runner processes each game to completion using the production GameRunner
and each game's recorded Chronicle; it does not retain entire Games across
samples. It uses independent deterministic streams for Grove choice,
mechanics, and Human Baseline tie breaking. A failed chunk must not be marked
complete. Existing expert job output is untouched.

The Kotlin runner runs through `runBaselineResyncCards` and can also be used
directly with `-Pstart=0 -Pgames=5 -Pplayers=4 -Poutput=...` etc.

The research runner and Gradle task have not been certified by a full Kotlin
compilation in the artifact-building environment; run the smoke test locally
and report errors before a multi-thousand-game batch.

## Progress and ETA (5B.12 research workflow)
The Bash launcher sources `bin/lib/experiment_run_timing.sh`, the same timing
helper used by previous experiments. It prints `Run X of Y`, elapsed time for
the previous *completed* chunk, mean duration of completed chunks, estimated
time remaining, and a local-clock ETA. These estimates begin after the first
newly completed chunk; completed/resumed chunks are skipped and excluded from
speed calculations. Each chunk also writes its complete Gradle log in
`<run directory>/chunks/sample-*.log`. A chunk runs silently at the top level
until it finishes, so the ETA updates at **chunk boundaries**, not after each
game. Prefer `CHUNK=25` or `CHUNK=50` for more frequent feedback at modest
Gradle startup overhead; `CHUNK=100` is fine for throughput. For live inner
output, `tail -f` the printed chunk log.

Run with your preferred streaming + saved-log convention:

```bash
PLAYERS=4 GAMES=2400 CHUNK=50 bin/human_baseline_resync_card_balance \
  |& tee ~/Downloads/human-resync-4p.log
```

`GAMES` and `CHUNK` are part of the run identity. To **resume**, keep their
values unchanged; changing chunk size creates a distinct run directory.

The separate `bin/human_expert_resync_batch` launcher now forwards the existing
Buy-expert Python orchestrator's per-learner ETA, duration, and 60-second
heartbeat through its outer `|& tee`, while also saving its own stage log.
This adjustment affects only **future launcher invocations**, not a process
that was already started. While an old invocation is running, watch its inner
log directly:

```bash
tail -f output/experiments/human-expert-resync/*/expert_buy_current_market.log
```

Do not install/change repository scripts while an active expert job is using
this checkout; extract and commit after it finishes.
