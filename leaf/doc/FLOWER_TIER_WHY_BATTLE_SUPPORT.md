# Flower 14/17 Why — Battle Support Ablation

## Purpose

This is the third single-family policy-context ablation for the current Flower-tier anomaly.

Observed expert-Buy concerns:

- 2-player Flower 17 is selected at about 1% or less when legal.
- 4-player Flower 14 is selected at about 0.2% when legal.

Prior ablations did not rescue the tiers:

- fresh learned Cultivation Main did not explain the anomaly;
- fresh learned Plant Effect / Targeting did not explain the anomaly.

Because player count most directly changes Battle, Battle Support is the next causal suspect.

## Controlled comparison

For each of the five current certified Buy learners at 2p and 4p:

1. Start Battle Support from zero weights.
2. Train Battle Support with that exact frozen certified Buy learner active.
3. Evaluate the frozen Buy learner with Human Battle Support.
4. Evaluate the same Buy learner with its freshly trained Battle Support policy.
5. Use the same mechanical, strategy, and random-Grove schedules for the two evaluation contexts.
6. Keep every other decision family Human Baseline.

The principal metric is Flower **selection when legal**, not raw exposure or raw purchases.

## Interpretation

A large rescue of the problem tier would indicate that Human Battle Support was suppressing the downstream strategic value of those Flowers.

If the problem tier remains near its current rate while neighboring Flower tiers remain healthy, Battle Support is not a sufficient explanation.

This experiment does not use Battle Main learning. Battle Main remains intentionally excluded because the existing learner has unresolved behavior around normal Plant activations.

## Fresh-policy discipline

Battle Support starts from zero. No old `data/ai/.../battle-support` weights are used.

The certified Buy policies are the current learners trained against the current Plant market, Sunlight system, and repaired Compost configuration.

## Timing

The experiment uses `leaf_experiments.timing.ExperimentPlanRunner`, the shared research orchestration timing layer. With the default 2p/4p × five learner setup there are 30 timed operations:

- 10 Battle Support training runs;
- 10 Human-Battle-Support evaluations;
- 10 learned-Battle-Support evaluations.

The common runner reports elapsed time, category-aware ETA, estimated finish, and child-process heartbeats.
