# D9 — Frozen Buy evaluation diagnostics integration

This stage uses the existing `runEvaluateBuyPolicy` evaluator and **never trains or promotes weights**.
Opt in to instrumentation with `--diagnostics-dir PATH`. Without that switch, the normal evaluator is unchanged.

For LEARNED games, the affected Buy policy is wrapped with an RNG-isolated Human Baseline shadow; both decisions see precisely the same request. The observer records same-state recommendations but only the learned choice is executed. CONTROL games are *not* shadowed: the disagreement reports describe **expert-reached states**, not baseline-reached states. Development histories are tracked for every player in both CONTROL and LEARNED games. The opponent policy is reported separately.

`--diagnostics-dir` writes D6 disagreement summary reports and six D7 development reports.

## Checks before full runs

1. Run `./gradlew test` (or at least D1–D8 focused suites), then a 2–4 sample smoke evaluation with `--diagnostics-dir`.
2. Run identical seeds and frozen weights with diagnostics enabled and disabled. Verify winner lists, final VP, Battle results, and existing market summary outputs match. The compile check by itself DOES NOT establish noninterference.
3. Only then run the large study; verify names, zero-acquisition cards, player counts and report arithmetic.

## Resumable launch (from repository root)

```bash
WEIGHTS=/absolute/path/to/frozen-official-buy.weights PLAYERS=4 GAMES=2500 CHUNK=50 \
  bin/evaluate_buy_disagreements |& tee ~/Downloads/buy-disagreement-4p.log
```

Run separately for 2p/3p/4p and each distinct frozen learner (assign separate OUT_DIR and seeds). Reusing an output directory with changed settings fails. Completed chunks have `.complete` markers and are skipped. Logs, raw per-chunk TSVs and aggregate combined TSVs are included in `upload-ready-results.tgz`. `config.lock` fingerprints weights and records the Git revision. The launcher insists on a clean tracked tree.

### Limits

* An instrumented run's `onEvent` callback retains observations in memory per evaluator chunk. Use modest chunks (e.g. 50 games) for long studies.
* The shell launcher uses Gradle `--offline`; the required distribution/dependencies must be installed locally.
* Merge computes percentages from summed counts, never averages chunk percentages. Detailed development per-player/card/die records are merged, while development aggregate summaries remain per-chunk. Subsequent reporting can rebuild aggregate summaries from merged rows.
* Current owner-state trackers are used as implemented in D4–D5; validate transfer and special-resource paths before treating asset totals as exhaustive.
* All reported card associations are observational and do not establish causal card strength.
