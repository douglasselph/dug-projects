# Learned Cultivation Main Policy

This research policy learns only the high-level Cultivation Build Main Action.
It is independent from the learned Buy policy.

## Decision boundary

The policy may choose among legal Main candidates:

- `DRAW`
- `PLANT:<stable card id>`
- `ROUND_EFFECT:1`
- `ROUND_EFFECT:2`

`DONE` is reserved as a stable action identity, but current Cultivation rules only
make Done legal after both normal Main Actions are already spent, so it is not a
Main candidate in the current coordinator.

The learner does **not** choose Plant/Round-effect targets. Human Baseline still
chooses dice, rows, cards, Mulch/Compost targets, payment, and all other lower-level
parameters. Optional Cultivation Support timing also remains Human Baseline.

## Model

The policy is a transparent linear action scorer evolved with the same small,
deterministic evolutionary style used by learned Buy. Standard features describe
visible player/game state and candidate type. Stable named features represent
Plant identity, effective Plant cost, and effective `GameEffect` identity.

Round and Plant experiment overrides are visible because features are extracted
from the experiment-aware `DecisionContext` and the effective Round effects in
`CultivationMainObservation`.

The v1 feature set deliberately does not import the Human Baseline action score as
an advisory feature. This keeps the first learner independent at the high-level
action choice while Human Baseline retains lower-level effect intelligence.

Global state features are accompanied by action/state interactions so a linear
scorer can respond differently to Draw, Plant, and Round actions as Battle timing
and remaining rounds change. Effective effects also receive context interactions
for Battle-next, Battles remaining, Sunlight held, and Main Actions remaining.

## Training

```bash
bin/train_cultivation_main_policy \
  --random-grove \
  --plant-overrides data/research/4p/resync/resync-current.csv \
  --round-overrides data/research/4p/round-overrides/sunlight-token.csv \
  --generations 8 --population 10 --games 50 \
  --output output/ai/cultivation-main-sunlight-1.weights
```

Default Buy is Human Baseline. To train Cultivation Main in an environment where
the affected player also uses a fixed learned Buy policy:

```bash
bin/train_cultivation_main_policy \
  --buy-policy learned \
  --buy-weights data/ai/4p/frozen-buy-first-game-default-v1.weights
```

That Buy policy is fixed; only Cultivation Main weights evolve.

Supported research controls include random/fixed Grove patterns, Plant overrides,
Round overrides, 2/3/4 players, explicit round structure, and independent
mechanical/strategy/evolution seed streams. Core runners never silently load the
provisional resync baseline.

## Held-out evaluation

```bash
bin/evaluate_cultivation_main_policy \
  --weights output/ai/cultivation-main-sunlight-1.weights \
  --random-grove \
  --plant-overrides data/research/4p/resync/resync-current.csv \
  --round-overrides data/research/4p/round-overrides/sunlight-token.csv \
  --games 1000
```

CONTROL and LEARNED use the same Grove, mechanical seed, strategy-seed base,
player count, and game rules for each matched sample. The affected role rotates
across physical seats. With `--buy-policy learned`, both CONTROL and LEARNED use
the same fixed learned Buy policy so the comparison still isolates Cultivation
Main selection.

Reports emphasize behavior and held-out outcomes rather than raw coefficients:
Main-action frequencies, Plant activations, effective Round-effect utilization,
Sunlight take rate, token economy, Plant/Battle/total VP, dice development,
wounds, and win share.

## Manifest and reproducibility

Saved policies record a versioned feature schema, Plant catalog fingerprint,
training seeds/configuration, and fitness. A trained policy refuses a changed
canonical Plant catalog rather than silently reusing stale identity knowledge.
Experiment overrides remain explicit run configuration and are not promoted into
canonical card data.
