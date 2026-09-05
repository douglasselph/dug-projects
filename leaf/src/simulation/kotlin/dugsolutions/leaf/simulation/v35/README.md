# v35 simulation source set

This source set is deliberately separate from both the rules engine and integration tests.

- `src/main`: rules, decision contracts, and the frozen Mechanical Baseline control.
- `src/test`: isolated unit correctness.
- `src/integration`: deterministic real-engine scenarios and scripted decisions.
- `src/simulation`: strategy experiments, large game batches, learning, persistence, and analysis.

Strategy levels are labels, not inheritance requirements. A profile may mix levels by decision area; for example a Level-1 Buy strategy can be compared against Level-0 behavior everywhere else.

The higher-level strategy namespaces are intentionally scaffolding only. Do not make the Mechanical Baseline smarter to prototype a new strategy. Add a new strategy implementation here and compose it into a `DecisionDirector`/`StrategyProfile` for the experiment.
