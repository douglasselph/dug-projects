# Phase B: scored Cultivation Main Actions (opt-in)

The existing Human Baseline remains the legacy reference unless a `GameConfig` opts into a `DecisionExperimentConfiguration`.

## Selection

- `LEARNED` keeps the existing `cultivationMain` policy.
- `LEGACY_HUMAN_BASELINE` keeps the existing `cultivation` reference Main choice.
- `HUMAN_BASELINE` scores every legal Main action with the new score model. It changes only Main selection; optional Support/Buy/Battle logic is untouched.
- `HUMAN_WHEN_COMPOST_AVAILABLE` switches to the scored Human Main selection only when one of the legal Round effects is a Compost variant. Otherwise the original Main policy decides.

The coordinator forwards the **effective RoundCard** to the Main policy. The policy is wrapped once per player when GameFactory creates a game, and gets an independent strategy RNG derived from the player's strategy seed with a constant salt. The mechanical RNG is not involved in scoring or tie-breaking. Score ranges are sampled once per candidate per decision and their realized values are retained in candidate reasoning, provided `recordDecisionReasoning=true` or `chronicleDetail=true`.

## Enabling an experiment

The two files under `data/ai/` are NOT read automatically. Explicitly load them into a `GameConfig` constructor:

```kotlin
val experiment = DecisionExperimentConfiguration.load(
    java.nio.file.Path.of("data/ai/decision-experiment.properties")
)
val scores = HumanScoringConfiguration.load(
    java.nio.file.Path.of("data/ai/human-scoring.properties")
)
val config = GameConfig(
    playerDecisionFactories = listOf(expertFactory, expertFactory),
    decisionExperiment = experiment,
    humanScoring = scores,
    recordDecisionReasoning = true,
    // Supply roundValues explicitly when researching non-canonical Compost mechanics.
)
```

Set `cultivation.main.policy=HUMAN_WHEN_COMPOST_AVAILABLE` to compare an otherwise learned expert with a scored Human Main policy only at Compost opportunities. Tune `compost.base` in `data/ai/human-scoring.properties` to increase or decrease Compost preference. There is no separate `external.*` preference layer.

## Calibration

All existing deterministic scores are preserved as the initial foundation: e.g., Compost base 75, +2 per added side, +2 per remaining Cultivation round (up to five), -10 per Buy threshold lost, -40 below Hand Buy floor, and existing dice-development bonus. The Phase B loader permits changing `compost.base`, `compost.upgrade_side`, `compost.future_round`, `compost.buy_tier_penalty`, `compost.low_buy_power`, and other action base scores.

Old Compost 75%/50%/10% willingness gates are NOT mathematically equivalent to any score range without accounting for opposing action margins. The new calibration STARTING points are:

- `compost.low_roll=-10..20` when the selected die shows 1 or 2
- `compost.ordinary_roll=-25..10` otherwise
- `compost.low_power_variation=-20..0` if the remaining Hand power is below the cost-5 floor
- `compost.repeat_commitment=-12..0` for a second Main commitment with two or fewer Hand dice

Mulch and Sunlight likewise use scored ranges rather than hard eligibility gates in the opt-in scored Main selector. All legal Main candidates remain in scoring, even at low scores. Overgrowth/Pocketed Spark and other optional Support behavior have NOT been migrated in this phase.

The Keep-no-rewards variant shares the same permanent upgrade value but offsets the old Discard opportunity costs and estimates the new roll's impact against the old face. Keep-with-rewards adds an initial +5 expected reward heuristic. These Keep adjustments are PROVISIONAL. Neither variant was calibrated from human play observations, so measure actual action rates and outcome margins before treating the score magnitudes as reliable.

## Limitations

1. This patch does not add a standalone batch experiment launcher, nor an automatic scan of `/data/ai` files. The caller controls experiment choice explicitly per GameConfig; no implicit environment mutation occurs.
2. The Main strategy currently inherits legacy contextual score calculations for all actions and exposes them as named influences (`*.legacy.N`). Those indices have deterministic evaluation order but should eventually be replaced by semantic stable IDs for every contextual influence.
3. `HUMAN_WHEN_COMPOST_AVAILABLE` changes the policy for **all Main candidates on that specific decision**, not Compost alone. A non-Compost Main choice may therefore differ from the expert's selection even when Compost is not selected.
4. Score metadata and mechanical decision replay remain separate features. Phase C will strengthen causal matching of later random events.
5. The new scored Main policy is only invoked after the enclosing Cultivation strategy requests a Main action; optional Support timing remains with that strategy.
