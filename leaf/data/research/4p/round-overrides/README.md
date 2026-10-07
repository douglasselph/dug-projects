# Round experiment overrides

Research-only Round-card effect replacements use a strict CSV with this shape:

```csv
round_card,effect_slot,effect
Resource_Sunlight_Compost,1,GAIN_SUNLIGHT_TOKEN
```

- `round_card` is the canonical Round card `name` from `Turn_Cards.csv`.
- `effect_slot` is `1` or `2`.
- `effect` is an exact non-`UNKNOWN` `GameEffect` enum constant.

No file means fully canonical Round-card effects. The production
`Turn_Cards.csv` remains authoritative and is never mutated by this facility.

Use with research runners via:

```text
--round-overrides <path>
```

## Compost "use it now" experiment

The proposed Compost repair is represented as a research-only Round override:

```text
data/research/4p/round-overrides/compost-use-now.csv
```

Use it with any train/evaluate runner that accepts `--round-overrides`:

```text
--round-overrides data/research/4p/round-overrides/compost-use-now.csv
```

That file is intentionally self-contained. It includes the currently accepted
Round resync interventions from `data/research/4p/resync/round-resync-current.csv`
plus all three canonical Compost slots rerouted from `UPGRADE_DIE_FROM_HAND` to
`UPGRADE_DIE_AND_USE_NOW`. This avoids changing the accepted current resync while
the Compost repair is still experimental.

If the repair is accepted as the new research baseline, copy the three Compost
rows into `data/research/4p/resync/round-resync-current.csv`. Until then, the
current resync remains the control condition and this file is the variant.

