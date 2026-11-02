# D6 — Same-state Buy disagreement reports

`BuyDisagreementReports.generate(events)` accepts D3 `BuyDecisionEvent` records and produces deterministic UTF-8 TSV content. `write(events, directory)` writes three files:

- `overall-disagreement.tsv`: all comparable Buy decisions, their classification, and the disagreement percentage; separate rows by player count, round, and purchase position.
- `card-disagreement.tsv`: every named Plant observed in the decision market, even if never recommended; both, baseline-only, expert-only, neither, and three conditional disagreement percentages.
- `purchase-substitutions.tsv`: exact directional pairs of Human Baseline and expert recommendations, including DONE and dice size. Denominator is the total disagreements for that player count.

## Denominators and interpretation

Overall disagreement % = (different item + expert buys/baseline done + baseline buys/expert done) / all same-state decisions. SAME_ITEM and BOTH_DONE are agreements.

Per-card disagreement when either recommends = (baseline only + expert only) / (both + baseline only + expert only). The two directional conditional rates are baseline-only / (both + baseline-only) and expert-only / (both + expert-only), respectively. Zero denominators are empty cells, **not zero percent**. `neither` counts all recorded decision states when neither policy selected that card; these states should not be construed as purchase opportunities.

Percentages are displayed on a 0–100 scale (not fractions). Decision observations are correlated within games; no significance or causal claims should follow from these TSVs alone. Market/legality is recorded for audit but does not define any reported percentage denominator.

## Group isolation

**Do not pool different trained learners, evaluation roles, state origins (expert-state versus baseline-state), or catalog fingerprints in a single input call.** D6 groups only by player count. D9 evaluation integration will partition data by cohort and attach run metadata.

The report aggregation is intentionally offline/opt-in. It does not execute either strategy, influence decisions, or alter RNG. D7 adds development summaries; D8 tests noninterference; D9 connects reports to the evaluator and archives.
