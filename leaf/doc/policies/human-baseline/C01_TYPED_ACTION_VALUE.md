# C01 — Typed ActionValue diagnostic model

Adds `ActionValue<T>` in Human Baseline `scoring/` alongside the existing `PriorityScore` and `DecisionScore`.

Each immutable record contains phase, effective `GameEffect`, typed candidate, optional target label, legality, calculated benefit, contextual bonuses and costs, realized uncertainty, realized decision variability, and final preference. Illegal actions have negative-infinity preference and cannot pass `requireLegal()`.

**Boundary:** No change to the policy's action selection, current priority heuristics, game mechanics, round cards, costs, trained weights, or research families. This is C01 schema/infrastructure only; it is not a claim that existing priority scores are already calibrated to pips or Battle VP. Existing `PriorityScore`/`ScoredChoice` can be adapted without changing their sums. Future tasks will populate genuine calculated benefits, use sampled uncertainty from strategy RNG, and instrument actual decisions.

Tests: `./gradlew test --tests '*ActionValueTest'`; `python3 bin/a03_verify_boundaries.py`.
