# D10 — Seeded noninterference pilot and frozen Buy-expert evaluations

## First gate: same-state diagnostics OFF versus ON

Use a **trained, official-current-market Buy weight file** from the previous 15-learner study. Do not use the older `data/ai/4p/...` files merely because they are present: their catalog or training provenance may differ. The evaluator rejects mismatched or untrained weights.

From the repository root:

```bash
WEIGHTS='/absolute/path/to/official-current-market-learner.weights' \
PLAYERS=4 GAMES=4 \
bin/pilot_buy_diagnostics_noninterference \
  |& tee ~/Downloads/d10-pilot-4p.log
```

This launches two **separately seeded, identical held-out evaluations**: diagnostics OFF and diagnostics ON. Only the diagnostics switch changes. It compares the complete MarketEvaluationJsonWriter JSON result, the entire evaluator's printed held-out report, and verifies the existence of all six expected diagnostic report files. On any difference, it exits nonzero and writes `verdict.json`, with a reason. The terminal prints the pilot archive only on a PASS. A failed pilot must block full evaluations.

The pilot is deterministic for the specified engine and weights; it is not a proof that every possible noninterference path is correct. Run for 2p, 3p and 4p with matching weights. A more exhaustive 20-game pilot is appropriate before long jobs.

Settings: `WEIGHTS`, `PLAYERS` (2/3/4), `GAMES` (default 4), `SEED` (9661000), `STRATEGY_SEED` (9761000), `OUT_DIR` (optional). Pick held-out seeds outside the weight file's training cohorts. The launcher uses `./gradlew --offline`, so its dependencies must be installed locally.

## Second gate: complete frozen-expert evaluations

After pilots PASS, run the existing D9 resumable launcher separately for each of the 15 existing trained policy weight files:

```bash
WEIGHTS='/absolute/path/to/official-current-market-learner.weights' \
PLAYERS=4 GAMES=2500 CHUNK=50 \
bin/evaluate_buy_disagreements \
  |& tee ~/Downloads/d10-frozen-buy-4p-learner1.log
```

Repeat for the other learners and player counts. The D9 runner skips completed chunks, prints progress and ETA, and prints the path of `upload-ready-results.tgz` after merging and verifying the archive. It requires a clean tracked Git tree; commit these scripts before the full run. It does not retrain or promote weights. Never combine different policy weights into a single run directory. Keep the pilot verdict with each learner's results for reproducibility.

## Interpretation

D6 decision disagreements use the exact state reached by the learned expert, with an isolated shadow Human Baseline. D7 development histories separately capture CONTROL, LEARNED and Human opponents. Comparisons are descriptive, not causal effect estimates. These studies do not evaluate whole-game expert policies, and a win-share increase from Buy-only optimization should not be confused with a guarantee of winning.

## Validation limitations

- The pilot verifies market JSON and printed reports; it does not compare every intermediate RNG draw or full event chronology. Discrepancies in final outcomes/market behavior should nonetheless fail.
- The source snapshot included no archived set of 15 official current-market learner weights. Obtain them from the earlier expert run's output tree and validate catalog fingerprints on launch.
- The original D9 runner's per-chunk seed increment and evaluation seeds should remain recorded and unchanged for comparisons.
