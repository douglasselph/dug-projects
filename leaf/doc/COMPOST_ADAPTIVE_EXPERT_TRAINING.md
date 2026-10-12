# Compost-adaptive expert training (Upgrade and Discard)

This study addresses whether a newly trained learned policy can sustain its win performance **while using Compost more**, not whether a fixed trained policy is disrupted by making Compost attractive after training.

## Mechanism

`FixedCompostBonusPolicy` adds an **absolute score offset** to the learned score of Round Effect 1 or 2 *only if* that Round Effect is one of the three Compost variants. All other learned scores are untouched. The offset is active **during each evolutionary candidate's training games** and again for paired post-training evaluation. All standard and named learned coefficients, including Compost-related coefficients, remain mutable: training can oppose/cancel the bonus.

Human Baseline has its own three-way scoring dispatch for `UPGRADE_DIE_FROM_HAND`, `UPGRADE_DIE_AND_USE_NOW_NO_REWARDS` and `UPGRADE_DIE_AND_USE_NOW`. That system is not used for Cultivation Main selection in this experiment. Other unlearned lower-level decisions still use Human Baseline, as in the existing Cultivation Main trainer.

## Running

From repository root:

```bash
./gradlew compileKotlin compileSimulationKotlin
GENERATIONS=2 POPULATION=4 TRAIN_GAMES=4 EVAL_GAMES=4 REPLICATES=1 BONUSES=0,0.03 CHRONICLES=0 bin/experiment_compost_adaptive_training
```

Full run, showing a live terminal log:

```bash
set -o pipefail
BONUSES=0,0.01,0.03,0.05,0.10 REPLICATES=3 GENERATIONS=12 POPULATION=12 TRAIN_GAMES=60 EVAL_GAMES=1000 \
  bin/experiment_compost_adaptive_training 2>&1 | tee ~/Downloads/compost-adaptive.log
```

This invokes Gradle for each training/evaluation job; budget and runtime can be substantial. Training is reproducible with fixed evolution, mechanical and strategy seeds per replicate. Report progress comes from the existing detailed evolution generation logs and the shared `ResearchProgressReporter` during evaluation. Completed training weights are reused on reruns unless `FORCE_TRAIN=1`.

## Outputs

`output/experiments/compost-adaptive/config/settings.txt` captures settings, `trained/` contains evolved main-action weights for each replicate and bonus, `evaluation/` contains per-seed paired TSV comparisons, `summary.txt` gives paired VP/margin and Compost usage, and `compost-adaptive-results.tar.gz` collects everything.

**Do not treat a positive bonus as an enforced Compost usage frequency.** The optimizer can evolve a negative Compost coefficient to compensate. Compare realized usage and VP/win results. If no high-usage strategy evolves, this experiment alone cannot prove one doesn't exist. For publication decisions, also run separately trained Buy and Main policies and multiple training replicates before attributing all differences to Compost.

## Scope/limitations

This version tests only Upgrade and Discard. It uses a frozen previously trained Buy policy; only the Cultivation Main policy evolves, with lower-level decisions remaining as in the pre-existing trainer. All training candidates face the same frozen initial learned Main/Buy expert opponent, rather than Human Baseline, using `--opponent-main-weights`. The reference zero-bonus policy is independently trained with matching budget. C2/C3 event-matched rolls are enabled in evaluation, but other random mechanics are not yet fully event-keyed. A broader full-system adaptive training of Buy, Plant Effect and Battle Support would be a separate experiment.
