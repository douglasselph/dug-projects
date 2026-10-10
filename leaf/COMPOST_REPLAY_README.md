# Compost seeded counterfactual research — instrumentation (v1)

This patch adds opt-in per-game Cultivation Main decision tracking and single-decision intervention. Ordinary runs, with `GameConfig.cultivationDecisionReplay == null`, take the old code path.

**API:** `CompostCounterfactualRunner(factory, runner, selectedPlantCards, decisionFactories, roundSetup)` in the simulation source set.

```kotlin
val study = CompostCounterfactualRunner(gameFactory, gameRunner, selectedPlantCards, decisionFactories)
val baseline = study.run(mechanicalSeed = 1234L, retainChronicle = true)
val compost = study.compostOpportunities(baseline).first()
val fork = CultivationReplayFork(
    compost.roundNumber, PlayerId(compost.playerId), compost.consultationIndex,
    ReplayMainAction.DRAW
)
val comparison = study.compare(1234L, fork = fork, original = baseline)
CompostReplayReportWriter.write(comparison, Path.of("output/experiments/compost-replays/game-1234"))
```

The paired output contains detailed human-readable Chronicles, compact final summaries, tabular decision traces, and a VP / dice / Plants / wounds comparison. Repeat with many distinct seeds and different *legal* alternative decisions to detect patterns. Each invocation must have independently constructed game-local strategy factories and a new tracker; do not reuse mutable policies across pairs. If the alternative is not legal at the fork, the runner fails rather than silently substituting a move.

Important limits:
- This is a **whole-game seeded restart**, not an in-memory midgame checkpoint or exact shared-future-randomness coupling. Once an alternate move changes the count of random draws, rolls in the continuations will differ; report that limitation when interpreting single-game stories.
- Each selected fork should be verified against the actual baseline: the comparison requires an original-rule Compost choice and a legal, distinct alternative.
- `ACTIVATE_PLANT` is intentionally excluded as a generic fork target because it needs a precise card identity. DRAW or either legal Round Effect can be compared now.
- The instrumentation is **library functionality**, not an unattended experiment CLI. Wire it into the experiment environment once the desired seed selection, policy matchup, and sample size are fixed.
- Detailed Chronicles are retained only when explicitly requested. For large surveys, use `retainChronicle=false` during preliminary screening, then rerun selected examples with it enabled.
- The writer's `summary.txt` uses Kotlin data-class formatting; TSV files are intended for machine processing.
