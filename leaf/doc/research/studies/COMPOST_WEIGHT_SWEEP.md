# Expert vs Expert — Compost score-weight sweep

This experiment compares two complete two-player games with matching starting seeds and C2/C3 keyed-percentile die rolls. Both seats use identical learned Main/Buy policies. In the *treatment only*, one rotating seat switches to **HUMAN_WHEN_COMPOST_AVAILABLE** when a Compost action is legal, scoring every legal Main Action (including Compost) with the Phase B Human scorer. Other decisions and the opponent's Main/Buy policy remain learned.

The Human scorer has an external Compost additive weight (not a probability); weights are swept independently. Control games use learned policies for both seats. The `decisionExperimentSeats` GameConfig option (zero-based) confines the policy routing to the affected seat. Null retains existing all-seat configuration semantics.

## Running

```bash
GAMES=10 WEIGHTS=-20,0,20 CHRONICLES=3 bin/experiment_compost_weight_sweep
```

`ORIGINAL_MAIN`, `ORIGINAL_BUY`, `KEEP_MAIN`, and `KEEP_BUY` select rule-matched learner weights. The original arm defaults to `data/ai/2p/*-original-compost.weights`. The keep arm is **skipped** unless its learned Main and Buy weights are supplied; it is scientifically incorrect to silently reuse original-rule weights as if they were experts under Keep. Example:

```bash
GAMES=1000 CHRONICLES=36 WEIGHTS=-20,0,10,20,40,60 \
KEEP_MAIN=path/to/keep/main.weights KEEP_BUY=path/to/keep/buy.weights \
bin/experiment_compost_weight_sweep
```

`ROUND_BASE` provides the existing experiment's other Round effect values, while the launcher overrides only the three Compost Round-card slots for each arm. `SCORING` points at `data/ai/human-scoring.properties`. Both arms are evaluated under separate RuleValueResolver configurations. The launcher writes `summary.txt`, `pairs.tsv`, saved control/hybrid Chronicles, config files, and a tarball in `output/experiments/compost-weight-sweep`.

## Interpretation

- `delta_vp` is affected-seat **hybrid VP minus expert control VP**. `delta_margin` similarly subtracts the opponent's score.
- `hybrid_compost_uses / hybrid_compost_opportunities` is realized behavior, not a prescribed percentage. Record the count as well as fraction (zero opportunities can occur).
- `first_policy_difference` reports the earliest observed divergence in that player's recorded decisions, which may not alone establish causal timing for other players.
- `matched_rolls` compares only keyed C2/C3 die rolls; `roll_mismatches` must be zero. Unmatched rolls are legitimate after a policy change. Wisp draws, shuffle state, and other unkeyed RNG are NOT event matched.
- Paired score changes are not pure isolated causal effects because some subsequent mechanical randomness is still sequential. Repeat with independent seeds and, ideally, independent learned policies.
- Phase B Human scoring ranges are provisional. No actual human probability is imposed.
- Source weight provenance matters: check both learned policies were trained or validated for the matching rule, player count, and Round/Plant environment.
