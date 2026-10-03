# Plant Experiment Overrides

Plant experiment overrides are a **research-only** way to change selected Plant acquisition economics and end-game scoring rules without editing the canonical Cultivation-card CSV files.

The canonical card data remains the definition of the actual game. An override file changes the effective Plant values only for a simulation/research run that explicitly loads that file.

## Quick start

Create a CSV anywhere on your filesystem, for example:

```csv
card_id,type,cost,available,scoring,effect
Vine_07_01,FLOWER,14,true,FIXED:3,RAISE_D8_PLUS_1
Vine_07_04,,7,true,PER_GRAFTED_FLOWER,RAISE_ANY_DIE_PLUS_1
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
card_id,type,cost,available,scoring,effect
```

Each row identifies one Plant by its stable card ID, such as `Vine_07_01`.

| Column | Meaning | Blank value |
| --- | --- | --- |
| `card_id` | Stable Plant card ID. Required for every nonblank row. | Invalid for a data row. |
| `type` | Experimental Plant type: `ROOT`, `VINE`, or `FLOWER`. | Use canonical type. |
| `cost` | Experimental acquisition cost. Must be an integer `>= 0`. | Use canonical cost. |
| `available` | Whether the Plant may appear in the Grove: `true` or `false`. | Use canonical availability (`true`). |
| `scoring` | Experimental typed end-game scoring rule. | Use canonical scoring. |
| `effect` | Exact `GameEffect` constant name. | Use canonical effect. |

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
card_id,type,cost,available,scoring,effect
Vine_07_01,,8,,,
```

Run:

```bash
bin/evaluate_buy_policy --plant-overrides data/research/plant-overrides/berry-important-cost-8.csv
```

The card remains available. Only its effective acquisition cost changes for that run.

You can use experimental costs that are not normal printed Plant tiers. For example, costs 8 and 10 are valid research values.

### Exclude a Plant

```csv
card_id,type,cost,available,scoring,effect
Vine_07_04,,,false,,
```

For random Grove evaluation:

```bash
bin/evaluate_buy_policy --random-grove \
  --plant-overrides /path/to/exclude-vine-yield.csv
```

Random Grove resolution will not select `Vine_07_04`.

### Exclude both known V7 outliers

```csv
card_id,type,cost,available,scoring,effect
Vine_07_01,,,false,,
Vine_07_04,,,false,,
```

For a random-Grove held-out evaluation:

```bash
bin/evaluate_buy_policy --random-grove \
  --plant-overrides /path/to/exclude-v7-outliers.csv
```

### Cost zero is not exclusion

```csv
card_id,type,cost,available,scoring,effect
Vine_07_01,,0,,,
```

This makes the Plant cost 0 for that experiment. It does **not** exclude the Plant.

Availability and cost are independent experimental dimensions.

### Explicit availability with a cost override

```csv
card_id,type,cost,available,scoring,effect
Vine_07_01,,11,true,,
```

This explicitly keeps the card available and changes its effective cost to 11.

### Override Vine Yield to score per Flower

```csv
card_id,type,cost,available,scoring,effect
Vine_07_04,,,,PER_GRAFTED_FLOWER,
```

Run:

```bash
bin/evaluate_buy_policy --plant-overrides /path/to/vine-yield-per-flower.csv
```

This leaves Vine Yield's canonical cost and availability unchanged. Only its effective end-game scoring rule changes for that experiment. Multiple copies each score from the same grafted-Flower count. The canonical `Vine_07_04` definition remains `PER_GRAFTED_VINE`.

### Combine independent dimensions

```csv
card_id,type,cost,available,scoring,effect
Vine_07_01,,11,true,FIXED:3,
Vine_07_04,,,,PER_GRAFTED_FLOWER,
```

Cost, availability, and scoring are independent. Blank fields continue to mean canonical values.

## Grove behavior

Availability is applied while resolving the Grove.

- Random Grove slots exclude cards with `available=false`.
- An explicitly fixed Grove request for an excluded card fails clearly.
- If exclusions leave no legal Plant for a required random Grove slot, setup fails clearly instead of silently falling back to canonical cards.
- Loading an override file does not itself consume gameplay RNG.
- Matched CONTROL/LEARNED evaluation uses the same resolved Grove and the same Plant experiment configuration within a paired sample.

Experimental `type` and `cost` together define Grove slot membership for random research setup. For example, overriding `Vine_07_01` to `FLOWER` cost `14` removes it from the V7 candidate pool and adds it to the F14 candidate pool. A fully random Grove still contains exactly one R5, R7, R9, V7, V9, V11, F11, F14, and F17 card.

Non-zero digits in an explicit `--grove` pattern still identify canonical numbered card IDs. If a named card has been moved by an active type/cost override so it no longer belongs to that slot, setup fails clearly; use `0` for that slot when testing tier/type moves.

## Training Grove selection

`bin/train_buy_policy` still defaults to the canonical fixed `FirstGameDefault` Grove, preserving historical training behavior when no Grove option is supplied.

For experiments that exclude cards from `FirstGameDefault`, train against an explicit Grove pattern or a random Grove:

```bash
bin/train_buy_policy --random-grove \
  --plant-overrides /path/to/exclude-v7-outliers.csv
```

or, for a partially fixed Grove:

```bash
bin/train_buy_policy --grove 000100000 \
  --grove-seed 81000 \
  --plant-overrides /path/to/plant-overrides.csv
```

`--random-grove` is equivalent to `--grove 000000000`. Every zero slot is resolved once per training sample using a dedicated Grove RNG. The default training Grove seed is `81000`; override it with `--grove-seed`.

The complete Grove schedule is deterministic for the Grove seed and sample index, and it is resolved before candidate evaluation. Therefore every candidate in every generation sees the **same concrete Grove for the same training sample**. Grove resolution does not consume mechanical or strategy RNG.

An explicitly fixed unavailable card still fails clearly. If exclusions make a random Grove slot impossible, training also fails clearly rather than silently restoring a canonical card.

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

Blank `type`, `cost`, `available`, `scoring`, or `effect` fields mean that property remains canonical. Old override files without a `type` column remain valid.

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


## Effect overrides

The optional `effect` column replaces the Plant's executable `GameEffect` for that research run.
Use the exact `GameEffect` enum constant name; blank keeps the canonical effect. Older four-column
override files remain valid. Example:

```csv
card_id,type,cost,available,scoring,effect
Vine_07_01,11,,,RAISE_LOWEST_DIE_PLUS_1
```

This changes only the research game environment. The canonical card data and `PlantCard.effect` remain
unchanged. Runtime Plant-effect access goes through the explicit per-game `PlantValueResolver.effectFor`
seam, just like experimental cost and scoring.


Research-only effect constants currently include `RAISE_LOWEST_DIE_PLUS_1` and `RAISE_D8_PLUS_1`. `RAISE_D8_PLUS_1` may be used only when the player has a D8 in hand; it raises a chosen D8 by +1.

## Provisional resync baseline

Ongoing Plant-balance work maintains an explicit working baseline at:

```text
data/research/resync/resync-current.csv
```

This file is **not** discovered automatically by `train_buy_policy` or
`evaluate_buy_policy`. Core runners still require an explicit
`--plant-overrides` path so ad-hoc and historical runs cannot silently change
when the resync baseline evolves.

Resync-aware experiment scripts merge the current baseline with their
experiment-specific intervention and pass one effective duplicate-free CSV to
the core runner. The merge semantics are:

- layers are applied left-to-right;
- nonblank fields in later layers replace earlier values for the same card;
- blank fields preserve the value already supplied by an earlier layer;
- cards not mentioned by a later layer keep their baseline intervention.

The helper is:

```bash
bin/merge_plant_overrides OUTPUT.csv BASE.csv [LAYER.csv ...]
```

The generic tier calibrator uses the provisional resync baseline by default:

```bash
bin/experiment_card_tier_calibration Root_09_03 screen --tiers 9,11,14,17
```

Use another preserved baseline explicitly:

```bash
bin/experiment_card_tier_calibration Root_09_03 screen \
  --baseline-overrides data/research/resync/resync-v01.csv \
  --tiers 9,11,14,17
```

Or intentionally return to canonical-only Plant data for that experiment:

```bash
bin/experiment_card_tier_calibration Root_09_03 screen \
  --no-baseline-overrides \
  --tiers 9,11,14,17
```

Result directories include a SHA-256-derived `resync-<hash>` component and
archive a copy of the baseline used. This prevents `.complete` markers or
weights from an older baseline from being silently reused after
`resync-current.csv` changes.

Before changing `resync-current.csv`, preserve an accepted checkpoint as a
numbered file such as `resync-v01.csv`.
