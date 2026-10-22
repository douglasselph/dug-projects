# 5B.12A — Human Baseline decision-distribution diagnostics

Run `bin/human_baseline_5b12a_decisions [output-directory]` from any directory. The script runs 12 fixed-seed four-player games and saves the scored-choice distribution, label-derived families, winning score margins, frequent adjustment reasons, Buy decision events, and up to 120 examples of selected and rejected alternatives. Use `./gradlew runBaselineDecisionDistribution -Pgames=... -Pplayers=... -PbaseSeed=... -PstrategySeed=... -Poutput=...` to vary the cohort. Run twice with different output directories and compare the generated TSV files via `diff` to confirm deterministic output.

## Interpretation and limits

- `selected-choices.tsv` is authoritative for recorded scored labels. `action-families.tsv` is only a string-label heuristic, **not** authoritative enumeration of all Draw/Plant/Round actions; labels are not typed actions. `other-or-subordinate` can contain many nested choices.
- `score-margins.tsv` compares the chosen action's total with the best rejected alternative (not merely the second sorted score). `tie-0` means score tie, not necessarily a problem.
- `score-adjustments.tsv` tallies adjustment reason frequency, not total impact. Examine `sample-explanations.txt` for actual values and alternatives.
- `buy-decisions.tsv` counts `GameEntry.BuyDecision` events separately; it does not infer every purchase from its labels and does not claim full Buy opportunity auditing. Buy outcomes and selected kind are recorded directly.
- The recording uses production `GameConfig(recordDecisionReasoning=true)` and existing typed Chronicle entries. Scoring/RNG/decisions are not modified. Chronicle traces are opt-in; no new score-affecting logic is introduced.
- Approval requires human inspection of action mix and sample explanations, alongside earlier 5B.12 correctness/reproducibility calibration. No automatic policy freeze or approval is asserted.
