# Compost-only learned-policy preference sweep

This experiment fixes a confound in `experiment_compost_weight_sweep`: the older hybrid replaced the learned Main policy with Human Baseline on all Compost-present decisions. This new experiment **never substitutes Human Baseline for other Main actions**. It uses the original learned Main score for all actions, with an adjustment to Compost alone.

## Definition of preference weight

At each Main choice, let `S(a)` be the original learned model score and `spread = max(1, max_a S(a) - min_a S(a))`. For Compost actions only, score is `S(Compost) + externalWeight * spread / 100`. Every other score is **unchanged**; ties follow the learner's existing legal-action order.

The weight is expressed as a percentage of the **learned score spread**, not the Human Baseline 75-point scale, and is **not** a probability of choosing Compost. Thus +20 raises Compost by 20% of the current range of learned scores. A very large positive weight may make Compost nearly inevitable, whereas negative weights discourage it. `weight=0` should exactly match the learned control for every seed; treat any nonzero difference as a regression.

The next phase could introduce a context-specific bonus calculated from Human Compost influences, but applying raw Human values directly to learned scores is invalid without calibration. This experiment deliberately isolates only an external preference applied to the learned score.

## Usage

From repo root, after unzipping patch:

```
./gradlew compileKotlin compileSimulationKotlin
GAMES=10 WEIGHTS=-20,0,20 PROGRESS_EVERY=5 bin/experiment_compost_only_preference
GAMES=1000 WEIGHTS=-20,0,10,20,40,60 CHRONICLES=36 bin/experiment_compost_only_preference 2>&1 | tee ~/Downloads/compost-only-preference.log
```

The launcher uses original policy weights at `data/ai/2p/` by default. Set `KEEP_MAIN` and `KEEP_BUY` to corresponding keep-no-rewards trained weights to enable that arm; it will not silently reuse original-rule weights.

The archive is `output/experiments/compost-only-preference/compost-only-preference-results.tar.gz`. `PROGRESS_EVERY` controls completed-seed reporting, default 25. The Kotlin `ResearchProgressReporter` emits run clocks, durations, ETA, and the established `RUN_COMPLETE`, `OVERALL_PROGRESS`, `PROCESS_COMPLETE` tokens. It is reusable by future simulation experiments, though the older Python timing library remains separate.

## Replay caveats

The compared games share seeds and opt into event-keyed/shared-percentile rolls. Replay event matching does not make distinct game events equivalent. Other mechanical RNG calls remain sequential. The affected seat alternates. Confidence intervals are computed using paired seed differences, not independent comparisons across weights. Variation due to later gameplay must not be described as completely eliminated.
