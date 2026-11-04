# v38-01 Card catalog / Compost synchronization (2026-11-04)

Baseline archive: `leaf(20261104-201517).tgz`.

## Canonical card sources

The two official Plant catalogs in `data/` were replaced byte-for-byte from the
provided source-of-truth CSVs:

- `Cultivation_Cards - Root.csv` ← `Cultivation_Cards - Root(6).csv`
- `Cultivation_Cards - VF.csv` ← `Cultivation_Cards - VF(6).csv`

The full catalog remains 12 Root + 24 Vine/Flower definitions; only the
`effect` field changed in 4 Roots and 11 Vine/Flowers. Changes chiefly remove
the display-only dice glyph, plus Root Recall's duplicated word and Bursting
Blossom's “any” → “a” wording. Vine and Punishment's new whitespace is already
accepted by converter normalization. All other official CSV fields are
unaltered (IDs, costs, VP, quantities, images, etc.).

`GameEffectConverter` now has **14 explicit mappings** for the previously
unrecognized v38 wordings, to the *same* `GameEffect` enum values as before.
The existing alias for Vine and Punishment already covers the 15th changed
entry. Original wordings remain registered for historical research overrides;
no global emoji stripping or fuzzy matching has been introduced.

The **Wisp's Resolve** source-of-truth entry was **already updated in the
provided baseline archive**: `data/Wisp_Cards.csv` contains
`<battle/> Battle Only: Immediately finish 1 Strike: Resolve → Doom → Cleanup.`
with quantity 2, and the converter already maps this exact wording to
`RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW`. That CSV and its mapping were left
untouched (no redundant change). Explicit regression assertions were added to
the converter/registry tests. The preceding Battle-engine patch owns the
actual execution of Wisp's Resolve.

## Compost (Cultivation Round effect)

- The next normal size is **not skipped** when unavailable.
- A selected die with an available next size upgrades exactly as before:
  replace it with the next size from the Graft Bed and put the replacement in
  Discard.
- A selected die whose next size is unavailable can instead be **Trashed**;
  choosing it for the Compost effect elects this v38 fallback. The shared
  `TrashResolver` applies the existing rule: a Trashed D4 returns to the
  Graft Bed; larger dice leave the game. A D20 has no next size and can be
  selected for the Trash fallback.
- The effect offers every die in the Hand to the strategy, including dice
  eligible only for Trash, even if another die could be upgraded. This avoids
  incorrectly forcing the strategy to choose an upgradeable die.
- Root Awakening and other special upgrade effects are **unchanged**.
- Human Baseline target evaluation recognizes the legal Trash fallback but
  **does not award it the permanent-upgrade development bonuses**. The new
  fallback receives a separate low valuation, so historical Upgrade heuristics
  do not automatically choose to destroy owned dice.

## Test coverage and local validation

Regression tests cover all 15 changed Plant texts, the current Wisp's Resolve
text/quantity, unavailable next-size and D20 Trash fallbacks, D4 return,
choosing a Trash target when another die is upgradeable, unchanged normal
upgrading, and valuation distinguishing Trash from a permanent Upgrade.

Verification completed here:

- Kotlin standalone compilation and mapping test of the **real**
  `GameEffectConverter` against all **72 populated effect fields** in the four
  official card data files: **PASS (no UNKNOWN effects)**.
- Kotlin standalone compilation and functional valuation smoke of the **real**
  `CompostPriority` with minimal dependency stubs: **PASS**.
- Full Gradle unit/integration/simulation tests: **NOT RUN** in the supplied
  environment because the Gradle 8.13 wrapper distribution is not cached and
  cannot be downloaded here. Run on Ubuntu before committing.

```bash
./gradlew test \
  --tests '*GameEffectConverterTest' \
  --tests '*WispCardRegistryTest' \
  --tests '*UpgradeEffectHandlerTest' \
  --tests '*DefaultGameEffectExecutorTest' \
  --tests '*CultivationMainPriorityTest' \
  --tests '*HumanBaselineEffectStrategyTest' \
  --tests '*HumanBaselineCultivationStrategyTest'

./gradlew integrationTest simulationTest
```

The entire research override directory, archived results, learned weights,
Battle scheduling, and launch architecture remain unchanged. Research results
should continue to identify source/rules checksums; moving to these revised
canonical card texts may legitimately change experiment fingerprints.
