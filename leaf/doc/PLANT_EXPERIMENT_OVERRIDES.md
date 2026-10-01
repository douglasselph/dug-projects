# Plant Experiment Overrides

Plant experiment overrides are a **research-only** way to change selected Plant acquisition economics and end-game scoring rules without editing the canonical Cultivation-card CSV files.

The canonical card data remains the definition of the actual game. An override file changes the effective Plant values only for a simulation/research run that explicitly loads that file.

## Quick start

Create a CSV anywhere on your filesystem, for example:

```csv
card_id,cost,available,scoring
Vine_07_01,11,,
Vine_07_04,,false,
```

Then pass the file to a supported research command:

```bash
bin/evaluate_buy_policy --plant-overrides /path/to/my-plant-experiment.csv
```

or:

```bash
bin/train_buy_policy --plant-overrides /path/to/my-plant-experiment.csv
```

When the option is omitted, Plant cost, availability, and scoring remain canonical.

## CSV format

The header is:

```csv
card_id,cost,available,scoring
```

Each row identifies one Plant by its stable card ID, such as `Vine_07_01`.

| Column | Meaning | Blank value |
| --- | --- | --- |
| `card_id` | Stable Plant card ID. Required for every nonblank row. | Invalid for a data row. |
| `cost` | Experimental acquisition cost. Must be an integer `>= 0`. | Use canonical cost. |
| `available` | Whether the Plant may appear in the Grove: `true` or `false`. | Use canonical availability (`true`). |
| `scoring` | Experimental typed end-game scoring rule. | Use canonical scoring. |

### Typed scoring expressions

The `scoring` field maps directly to the existing structured `PlantScoringRule` model. Supported expressions are:

```text
FIXED:<non-negative integer>
PER_GRAFTED_VINE
PER_GRAFTED_FLOWER
PER_BUTTERFLY
PER_OWNED_D4
```

For example, `FIXED:3` means a fixed 3 VP per copy. `PER_GRAFTED_FLOWER` means each copy scores 1 VP for each Flower grafted to that player's Creature. Unknown or malformed expressions fail clearly. A bare integer such as `3` is **not** valid scoring syntax.

The canonical card CSV and its `vp_icon` translation remain authoritative when `scoring` is blank.

## Examples

### Change only Berry Important's cost

```csv
card_id,cost,available,scoring
Vine_07_01,8,,
```

Run:

```bash
bin/evaluate_buy_policy --plant-overrides data/research/plant-overrides/berry-important-cost-8.csv
```

The card remains available. Only its effective acquisition cost changes for that run.

You can use experimental costs that are not normal printed Plant tiers. For example, costs 8 and 10 are valid research values.

### Exclude a Plant

```csv
card_id,cost,available,scoring
Vine_07_04,,false,
```

For random Grove evaluation:

```bash
bin/evaluate_buy_policy --random-grove \
  --plant-overrides /path/to/exclude-vine-yield.csv
```

Random Grove resolution will not select `Vine_07_04`.

### Exclude both known V7 outliers

```csv
card_id,cost,available,scoring
Vine_07_01,,false,
Vine_07_04,,false,
```

For a random-Grove held-out evaluation:

```bash
bin/evaluate_buy_policy --random-grove \
  --plant-overrides /path/to/exclude-v7-outliers.csv
```

### Cost zero is not exclusion

```csv
card_id,cost,available,scoring
Vine_07_01,0,,
```

This makes the Plant cost 0 for that experiment. It does **not** exclude the Plant.

Availability and cost are independent experimental dimensions.

### Explicit availability with a cost override

```csv
card_id,cost,available,scoring
Vine_07_01,11,true,
```

This explicitly keeps the card available and changes its effective cost to 11.

### Override Vine Yield to score per Flower

```csv
card_id,cost,available,scoring
Vine_07_04,,,PER_GRAFTED_FLOWER
```

Run:

```bash
bin/evaluate_buy_policy --plant-overrides /path/to/vine-yield-per-flower.csv
```

This leaves Vine Yield's canonical cost and availability unchanged. Only its effective end-game scoring rule changes for that experiment. Multiple copies each score from the same grafted-Flower count. The canonical `Vine_07_04` definition remains `PER_GRAFTED_VINE`.

### Combine independent dimensions

```csv
card_id,cost,available,scoring
Vine_07_01,11,true,FIXED:3
Vine_07_04,,,PER_GRAFTED_FLOWER
```

Cost, availability, and scoring are independent. Blank fields continue to mean canonical values.

## Grove behavior

Availability is applied while resolving the Grove.

- Random Grove slots exclude cards with `available=false`.
- An explicitly fixed Grove request for an excluded card fails clearly.
- If exclusions leave no legal Plant for a required random Grove slot, setup fails clearly instead of silently falling back to canonical cards.
- Loading an override file does not itself consume gameplay RNG.
- Matched CONTROL/LEARNED evaluation uses the same resolved Grove and the same Plant experiment configuration within a paired sample.

Changing a Plant's experimental cost does **not** move it to a different Grove slot. Grove slot identity is still based on the canonical Plant type/cost classification. For example, changing `Vine_07_01` from cost 7 to cost 11 still leaves it as the V7-slot card; it merely costs 11 to acquire during the experiment.

## Training caveat: FirstGameDefault

`bin/train_buy_policy` currently trains using the existing fixed `FirstGameDefault` Grove.

Therefore a cost override for a Plant in that Grove works, but excluding a Plant that is explicitly required by `FirstGameDefault` causes training setup to fail clearly. For example, excluding `Vine_07_01` currently conflicts with the fixed training Grove.

That failure is deliberate: the override system will not silently replace an explicitly requested card with a random alternative.

A future training configuration can expose a different/random Grove explicitly; until then, use exclusions in evaluation only when the chosen Grove can legally satisfy them.

## Reporting

When a Plant override file is active, supported research runs print the resolved intervention near the beginning of their output, using canonical values from the current card data. For example:

```text
PLANT EXPERIMENT OVERRIDES
source: /path/to/experiment.csv

Vine_07_01
  cost: 7 -> 11

Vine_07_04
  available: true -> false
  scoring: PER_GRAFTED_VINE -> PER_GRAFTED_FLOWER

All unspecified Plant properties canonical.
```

Keep this report with saved experiment output. It makes the intervention interpretable later even if the CSV file is moved.

## Validation and failure behavior

The loader fails rather than silently falling back when it encounters invalid experiment data. It rejects at least:

- unknown Plant IDs;
- duplicate Plant rows;
- malformed or negative costs;
- malformed `available` values;
- missing required CSV columns;
- malformed quoted CSV;
- unknown or malformed typed `scoring` expressions.

Valid Boolean values are `true` and `false` (case-insensitive after trimming).

Blank `cost`, `available`, or `scoring` fields mean that property remains canonical.

## Suggested location for checked-in experiments

For reusable experiments, a descriptive repository path is:

```text
data/research/plant-overrides/
```

For one-off experiments, the file may live anywhere; `--plant-overrides` accepts an arbitrary filesystem path.

Examples of useful filenames:

```text
berry-important-cost-8.csv
berry-important-cost-9.csv
berry-important-cost-10.csv
berry-important-cost-11.csv
v7-known-outliers-excluded.csv
```

The file location does not change its semantics.

## What the override affects today

Implemented now:

- Plant Buy legality/affordability through effective acquisition cost;
- Plant payment/purchase cost;
- Human Baseline Buy evaluation where Plant cost matters;
- learned Buy features/evaluation where Plant cost matters;
- research purchase-cost reporting;
- random/fixed Grove availability filtering;
- `--plant-overrides` in `evaluate_buy_policy` and `train_buy_policy`;
- typed scoring-file parsing into `PlantScoringRule`;
- experimental `PlantScoringRule` use by `FinalScorer`;
- resolved intervention reporting for cost, availability, and scoring.

Therefore the same override file can be used for **cost**, **availability/exclusion**, and **typed end-game scoring** experiments while leaving canonical card data unchanged.
