# Official card CSV migration — candidate, October 2026

Source of truth: user-supplied Root, VF, Wisp and Round_Cards.csv. Preserved these CSVs verbatim as `data/Cultivation_Cards - Root.csv`, `data/Cultivation_Cards - VF.csv`, `data/Wisp_Cards.csv`, and `data/Turn_Cards.csv` respectively.

- IDs remain alphabetically assigned **from the official CSV**. The leading `z` on `zFlower_` is dropped **only at runtime** in PlantCardRegistry so existing `Flower_...` code paths work. The CSV stays byte-identical to the original Tabletop Simulator export.
- Added exact-wording aliases to GameEffectConverter; these must not be fuzzy matched.
- Reassigned per-card Human Baseline scorer registrations to the official alphabetical IDs, and corrected default effect enums for changed official rules.
- The default Petal effect is `GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW` (generic official starting point); the P1–P4 variants remain available solely as research alternatives. The default Sapping effect is S1, per the printed official card.
- Historical `resync-current.csv` and `round-resync-current.csv` are saved as `*-pre-official-20261023.csv`. The active files now contain **headers only**, i.e. no overrides.
- Research scripts with ID-specific hypotheses and historical tests may still require migration. Do not treat old card-ID/weight fingerprints as comparable with the official layout. Do not overwrite currently running experiment source files.

## Validation before promotion

```
./gradlew test --tests '*OfficialCardSourceTest' --tests '*GameEffectConverterTest'
./gradlew test integrationTest simulationTest
bin/validate_plant_slot_market data/research/resync/resync-current.csv
```

Verify complete 2-player and 4-player smoke simulation *without any explicit overrides*, followed by a research smoke with the header-only overrides. Ensure no Kotlin test continues expecting the **old** card-to-ID mapping. The `FirstGameDefault` 5/7/9 tier layout is preserved, but its `Root_07_04` now correctly refers to **Root Loot**, not the former Root Recall; review teaching-set card selections.

**Not yet a validated production migration:** this package requires Gradle tests and full source-ID reference review before acceptance. Past learned Buy weights may encode the historical ID-to-effect mapping and must be evaluated/retrained under the new catalog.
