# C5 — replay validation and covered-roll diagnostics

## Installation and running

Run `./gradlew compileSimulationKotlin compileSimulationTestKotlin` and `./gradlew simulationTest --tests 'dugsolutions.leaf.simulation.v35.replay.ReplayEquivalenceDiagnosticsTest'` (if your simulationTest task supports filtering). Run `GAMES=10 bin/experiment_replay_equivalence_c5` for real-game smoke validation; increase GAMES after passing.

The launcher produces `output/experiments/replay-equivalence-c5/pairs.tsv` and `summary.txt`. Every seed executes two identical 2-player Human Baseline games with event-keyed, shared-percentile die rolls enabled; exact decisions, Chronicle entries, and score summaries are checked. A legal Main Action is then replaced with Draw, when possible, and keyed roll events are compared. Unexpected changes in matching roll percentiles fail the run. Control-only/comparison-only events are expected after a fork.

## Coverage and limitations

`ReplayRollEvent` is emitted only for rolls through `EventRollRandomness`. Its key is `(round, player, stable die ID, occurrence)`; ordinal is an ordering aid, not the identity. The ledger provides number of matching same-size events, cross-size coupled events, unexpected mismatches and events exclusive to either game. An equal-size event with shared-percentile mode must preserve both `u` and its face. A cross-size event must preserve `u`, not necessarily the face.

The ledger does not cover Wisp draws, shuffles, newly allocated die identities that diverge between histories, or random mechanics bypassing the event roller. Game-level exact comparisons include all Chronicle events and summaries, but may not be suitable for externally mutable or nonsemantic Chronicle metadata in the future. A forked game changing legal opportunities is legitimate, not a replay failure. This does not prove causal attribution of individual VP changes.

The `CompostCounterfactualRunner` accepts two additional optional constructor flags `eventBasedDieRolls` and `sharedPercentileDieRolls`, both default false to preserve old experiments. Use both true to enable C2/C3 coupling. Results made before this patch were not retrospectively event matched.

## Status

Kotlin compilation and the full game smoke runner must be executed on Ubuntu before using C5 evidence for balance claims. This patch changes no dice faces or rules in default mode and only records covered roll events in event-keyed mode.
