# A05 — Sunlight source of truth (2026-10-25)

## Status and invariant

**Canonical Cultivation Sunlight is `Gain Sunlight token`, never `Raise a die +3`.**
The authoritative definition is `data/Turn_Cards.csv`: all three `Resource_Sunlight_*` cards contain `effect_1_text=Gain Sunlight token`. The `GameEffectConverter` maps that exact wording to `GameEffect.GAIN_SUNLIGHT_TOKEN`; `RoundCardRegistry` loads it without inventing card-identity-specific rules; `RoundValueResolver.CANONICAL` preserves it. `DefaultGameEffectExecutor` routes the resolved effect to `ResourceEffectHandler`, which uses `SunlightTokenResolver.gain` to transfer a token from shared Grove supply to the player and records the transaction. It does **not** modify a die.

The tests added under A05 check all three actual CSV records, canonical resolution, and actual effect execution with a visible die that must remain unchanged.

## Legacy code — identified, NOT removed by A05

The **obsolete, incompatible** `RAISE_DIE_PLUS_3` compatibility path still exists in `GameEffect`, `GameEffectConverter` (the older printed text), `DefaultGameEffectExecutor` / `DieValueEffectHandler`, `GameEffectDecisionRequirements`, and Human Baseline scoring/targeting (including `SunlightPriority`). Some persisted learned weight files and tests reference the enum. **This is not a valid alternative canonical Sunlight rule.** A05 does not delete it, change learned features or weights, or alter mechanics. In particular, an artificial call that directly requests `RAISE_DIE_PLUS_3` may still execute that old behavior; this must not be represented as fully retired.

**Follow-on cleanup is required** before claiming Raise +3 has been removed from code: retire the obsolete converter alias, mechanic, scorer, targeting, and affected compatibility tests carefully; review persisted weight-feature compatibility before deleting/renaming any enum value. That is a separately reviewed protected-code change under A03, not a silent part of the Human Baseline recalibration.

## Reproduction

```bash
python3 -m unittest discover -s bin/tests -p 'test_a05_sunlight_source_of_truth.py' -v
./gradlew test --tests '*A05SunlightSourceOfTruthTest'
python3 bin/a03_verify_boundaries.py
```

A03 boundary checker must pass without exceptions: A05 adds only tests and documentation. No canonical rules, CSV entries, research overrides, learned policy families, frozen weights, round structure, or Grove selection logic are changed.
