# Market Substitution Diagnostic

Purpose: when a Plant was a legal Buy but the learned Buy policy rejected it, measure the exact alternative chosen instead.

Run after a certified current-market confirmation has produced the trained learner weights:

```bash
bin/experiment_market_substitution_diagnostic \
  --source-root output/experiments/current-market-confirmation-certified \
  |& tee ~/Downloads/leaf_market_substitution_diagnostic.out
```

This script does **not retrain** the learners. It reuses the 15 certified Buy-policy weight files and reruns only the matched held-out evaluations using the same current Plant/Round baselines and deterministic held-out seed schedule.

Structured market JSON schema v3 adds `rejectedLegalAlternatives` for every Plant. For each Buy decision where a target Plant was legal but not selected, the telemetry records the actual outcome:

- another Plant, with exact identity and cost;
- a die, with exact item identity and cost;
- `PLAYER_DONE` if the player deliberately stopped buying.

The invariant is exact:

```text
sum(rejectedLegalAlternatives.count) == legalDecisionCount - selectedDecisionCount
```

Primary reports:

- `reports/card-substitution-summary.tsv` — aggregated substitution choices by target card/player count/role.
- `reports/card-substitutions-by-learner.tsv` — same data separated by learner.
- `reports/card-opportunity-summary.tsv` — existing affordability/graftability/legality context.

The main questions to inspect first are 4p Flower 14 and 2p Flower 17. If Flower 14 is mostly replaced by Flower 17, that supports tier crowd-out. If it is commonly replaced by cheaper Roots/Vines/Flower 11 or dice, that instead points toward poor value at cost 14. Likewise, the 2p Flower-17 substitutions show what expert Buy policies prefer even when a 17-cost Flower is actually legal.

Human-readable evaluator output remains diagnostic only. Research reports consume structured machine-readable telemetry.
