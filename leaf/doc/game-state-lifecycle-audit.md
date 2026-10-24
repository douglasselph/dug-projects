# Task 2 — Game state ownership and cleanup audit

Source snapshot: `leaf(20261024-031330).tgz`.

## Scope and findings

- **Koin** (`di/AppModule.kt`, `game/di/GameModule.kt`, other `*/di/*Module.kt`): application-wide singletons are the registries/managers, resolvers, engine coordinators, effect executor, scorer, factory, and runner. There is no singleton `Game`, `Grove`, `RoundDeck`, or `GameChronicle` binding. Engine coordinators were inspected for state fields: round action/result collections are local to their calls.
- **GameFactory** (`game/di/GameFactory.kt`): creates `DieFactory`, `PlayerFactory`, `GameChronicle`, Players, strategy RNGs, decisions, Grove, RoundDeck, and mechanical intervention for each call. It does not store the constructed `Game`. Each `Game` constructs its own event RNG (when enabled) and `AssetProvenance`.
- **GameRunner** (`game/GameRunner.kt`): holds only shared coordinator/scorer; takes a `Game` argument and returns final scores/count. It never assigns the Game to an instance field. Final scoring details go into the per-game chronicle.
- **Chronicles**: `GameChronicle` owns entries, nested scope buffers, and sequence within its instance; `GameFactory` creates a new instance each game. Entries can be large; caller must not retain `Game` or an entries list beyond processing. Do not clear before TSV extraction/reporting.
- **Shared token accounting**: `Grove` creates a new `SharedTokenEconomyTracker` and hands it to its own Critters/Tokens/Butterflies. This observer is not a global subscription; it is owned and collected with that Grove.
- **Policies**: `PlayerDecisionFactory` is shared configuration; it is called to construct live `DecisionDirector` instances per player/game. Human baseline scoring/planners have mutable per-instance caches. Custom experimental decision factories must *also* return fresh mutable policies per call and not capture prior `Game` objects.
- **Card registries/managers**: application-wide, intentionally retained to avoid reloading CSV every game. Registry load/clear and manager load/clear **are mutating APIs**; freeze them by convention for the entire batch after initial loading. Do not reload catalogs mid-experiment.
- **Static/companion state**: domain companions and helper objects mostly provide constants, construction, and pure calculations. Targeted search did not expose a game-history singleton in the production game path. A code audit does not establish a heap-retention bound: use a long-run heap profile in the future persistent-JVM task.

## Ownership contracts for persistent JVM work

1. Create one Koin application and load the plant/round/wisp catalogs once per experiment (then do not mutate them).
2. Create a *new* `Game` using `GameFactory` for every sample; never call `run` on an already completed Game.
3. Share `GameRunner`, engine coordinators, immutable card definitions and frozen policy **weights**; never share mutable player-policy instances or game RNGs.
4. After a game finishes, emit all game-derived rows/telemetry **before releasing references**. Keep only aggregate counters and small immutable summaries needed by the experiment; do not append Games/Chronicles to experiment-wide lists.
5. Limit game and chronicle references to a local game-execution function or lexical scope. Never store them in a singleton, process-wide listener, or long-lived callback. Close each experiment-level Koin application when that experiment ends.
6. Do not invoke `System.gc()` per game or clear a Chronicle during final-score extraction; the JVM is responsible for reclaiming unreachable objects.
7. If training evolves weights across games, distinguish intended experiment-level learner state from forbidden game-state retention. Verify explicitly for every training driver when migrating it.
8. Track heap *after a full GC* across progressively longer runs separately from RSS and allocation throughput. Avoid claiming no leak from short tests.

## Tests and remaining risk

`GameIsolationLifecycleTest` verifies that successive Games made by one Koin container/factory do not share the key mutable objects; same seed/config gives the same final result and chronicle after independent execution; and the same `GameRunner` processes multiple other Games.

**Not yet proved:** constant retained heap over 10,000+ samples, every experimental learned-policy factory, every replay/counterfactual tool, thread safety during parallel simulation, and complete event-listener cleanup in non-baseline workflows. These require workload-specific tests/profiling; Task 2 does not silently assert them.

**Decision:** No production-state-clearing code change is justified in this snapshot. Add isolation regression coverage and enforce short-lived game references in the coming orchestrator, rather than risk changing simulation correctness through eager manual cleanup.
