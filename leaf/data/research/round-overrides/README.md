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
