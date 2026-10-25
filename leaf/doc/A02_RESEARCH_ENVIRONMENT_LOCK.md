# A02 — Research environment guardrails

Scope: preserve the intended 3/2/2 research default and legal nine-slot Plant Grove while preparing scoring calibration. This does **not** alter card effects, score priorities, simulation RNG, training, or the R3B controls.

## Round cadence

`GameRoundSetup.standard()` is `3 Cultivation / Battle / 2 Cultivation / Battle / 2 Cultivation / Battle`: 7 Cultivation and 3 Battle rounds (10 total). Main training/evaluation CLI defaults are 3/2/2. Deliberately supplied alternative `--rounds` / `GameRoundSetup.patterned` configurations remain supported. Do not use the older 2/2/3 rulebook wording as the research default.

## Catalog versus Grove

The canonical Plant CSVs must supply **36 distinct cards**, with exactly **four** alternatives in **each of nine legal type/cost slots**:

- Roots: 5, 7, 9
- Vines: 7, 9, 11
- Flowers: 11, 14, 17

A **single game Grove** selects exactly **one** Plant from each slot, for nine selected Plant stacks; it does **not** contain all 36 cards simultaneously.

Research-only cost/type/availability resyncs can change effective slot membership; validate the effective complete 4x9 catalog with `bin/validate_plant_slot_market` *before* experimenting. Do not treat the canonical name prefix as proof of the effective type/cost after a resync.

`PlantMarket` now validates one actual card per effective type/cost slot, beyond the pre-existing nine-card/three-per-type/name checks. `GrovePlantCode.encode` now rejects extra/duplicate cards and cards re-tiered/retyped away from their canonical numbered slot; `GrovePlantCode.describe` still falls back to stable IDs for research Groves.

`r0_audit_inputs.py` now validates 4 candidates in every canonical slot, with numbered 01..04 alternative IDs, rather than validating only total count 36.

## Verification commands (from the leaf project root)

```bash
python3 -m unittest discover -s bin/tests -p 'test_*.py' -v
python3 bin/r0_audit_inputs.py
bin/validate_plant_slot_market data/research/resync/resync-current.csv
./gradlew test --tests '*A02ResearchEnvironmentTest' --tests '*PlantMarketTest' --tests '*GrovePlantCodeTest'
bin/r3b1_effect_isolation --plan
```

The R3B.1 planning command should preserve the current two nine-card calibration Groves and 4,608-game plan for the previously used 2-generation, 4-population, 8-game, 80-holdout configuration. The default invocation will have a different game count unless those parameters are supplied.

## Acceptance

- Default game-round pattern is `3/2/2` in exact C-C-C-B-C-C-B-C-C-B sequence; explicit variants remain possible.
- The canonical 36 Plant CSV entries contain four legal candidates in each of the nine slots.
- The game cannot instantiate a selected Grove with duplicate, missing, or illegal cost/type slots even if it has nine distinct cards and three of each type.
- R3A's per-card Grove generation covers all 36 targets with one card in every slot and preserves deterministic seed behavior.
- No canonical Plant effects, research intervention definitions, or frozen AI weights change.
