# Task 7 supplementary fairness diagnostic

The `battle_pacing_task7_fairness_extension.py` script uses the existing Task 7 output files and introduces no Kotlin or game-mechanic changes.

Run after the normal Task 7 report:

```bash
python3 doc/tools/battle/battle_pacing_task7_report.py output/experiments/battle-pacing/task7-pilot
python3 doc/tools/battle/battle_pacing_task7_fairness_extension.py output/experiments/battle-pacing/task7-pilot
```

It creates `task7_fairness_extension.md`. The report computes opening-strength-stratified last consequential actor swing rates, common-support/Plant/strength strata comparisons, and *absolute* player-level VP effects of the controlled B starter rotation. Its fingerprint audit checks all counterfactual opening hashes against the paired experiment, and reports coverage. All comparisons are explicitly labeled observational unless based on the within-B order intervention. Do not interpret a within-B order intervention as a causal estimate of excess B versus A last-mover advantage.

For serious inference, first run a 100-pair per-cohort experiment using `--task7-counterfactual true`, and separately add a comparably implemented A scheduling intervention and High-Strike Wound cascade counterfactual. The A/B starting-state paired comparison alone cannot identify a causal advantage of being last because who becomes last meaningful actor is determined by decisions and resources during play.

No canonical rules or source were modified by this patch. This is an additive analysis and verification tool, not the complete Task 7 implementation.
