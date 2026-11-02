# Current research resync inputs

This directory contains **opt-in research configuration**, separated by concern.

## Plant overrides

`resync-current.csv` is the current Plant override layer.

In the November 2, 2026 source snapshot it is intentionally **header-only** after the October official-card migration:

```text
card_id,type,cost,available,scoring,effect
```

That means the canonical official Plant CSVs currently supply the effective card definitions unless an experiment explicitly passes another Plant override.

Historical resync snapshots remain in this directory for provenance. Do not assume an old intervention is still active merely because a historical CSV exists.

## Round overrides

`round-resync-current.csv` is the current Round-card override layer.

It is likewise currently **header-only**:

```text
round_card,effect_slot,effect
```

Experiments that need a Round variation should pass the intended override explicitly and record its fingerprint.

## Game-level rules

Game-level experimental rules do **not** belong in the Plant or Round CSVs.

Current Battle-structure selection lives in:

```text
game-rules-current.properties
```

with:

```properties
battle.structure=CANONICAL
# or THREE_ENGAGEMENTS
```

See `doc/battle/BATTLE_STRUCTURE_CONFIGURATION.md` and `doc/battle/BATTLE_PACING_RESEARCH_STATUS.md`.

## Loading discipline

The Plant/Round resync CSVs are research inputs, not a second canonical card database. Core commands do not silently merge arbitrary historical overrides.

Before serious research, record:

- source revision / clean-dirty state;
- canonical card fingerprints;
- effective Plant and Round override fingerprints;
- `game-rules-current.properties` contents when relevant;
- selected Grove;
- policy/weight provenance and seeds.

Canonical mechanics/data remain unchanged unless an experiment explicitly opts into a research variation.
