# A06 — Lock R3B/R3B.1 experimental control identities

**Status:** Checked in current source; regression guard added. This step does **not** change experimental mechanics, Human Baseline scoring, canonical card data, the 3/2/2 research rounds, 4×9 market legality, learned policies, or frozen weights.

## Authoritative control map

| Stable Plant ID | Actual Plant | Printed/canonical effect | Disabled research control | Exaggerated research control | Separate one-sided control |
|---|---|---|---|---|---|
| `Root_05_02` | **Root Four More** | Raise a die +4 | `RESEARCH_NO_EFFECT` | `root-raise20` → `RESEARCH_RAISE_DIE_PLUS_20` | `research-vp20` → `RESEARCH_GAIN_TWENTY_VP` |
| `Flower_17_04` | **Queen’s Blossom** | Draw 2 dice | `RESEARCH_NO_EFFECT` | `queen-draw4` → `RESEARCH_DRAW_FOUR_DICE` | `research-vp20` → `RESEARCH_GAIN_TWENTY_VP` |
| `Root_05_04` | **Root Well** | Gain 1 Water; optional Battle Water-spend reroll | **Not an R3B/R3B.1 target** | **No Raise +20 control** | **Not an R3B.1 VP20 target** |

- The `+20` in **Raise +20** means a change to the *die-raising amount*, capped by physical die sides. It does **not** mean 20 VP, and it applies to **Root Four More**, not Root Well.
- **Draw 4** is a Queen’s Blossom research exaggeration of canonical Draw 2. Actual draws can be limited by dice supply and legal Battle placement.
- **Gain 20 VP** means an immediate award of precisely 20 VP **per effect resolution**. It is a *separate, positive calibration control*, run one-sided for each target card, not a replacement for Raise +20 or Draw 4 in the R3B discovery phase.
- `disabled` is the effect no-op, not removal of the Plant's printed end-game VP or market availability.
- R3B's `extreme` label refers to the appropriate target-specific exaggeration, **not** a universal effect.
- In the R3B.1 one-sided tests only the designated targeted player receives the override. Untreated opponents remain canonical; the unrestricted-vs-baseline matchup must remain byte-identical.

## Provenance and regression guard

Official Plant names/printed effects are read from `data/Cultivation_Cards - Root.csv` and `data/Cultivation_Cards - VF.csv` using the existing R3A catalog loader. Target-to-variant mapping lives in `bin/r3b_effect_sanity_gate` and `bin/r3b1_effect_isolation`. R3A's `--effect-variant` preflight rejects assigning `root-raise20` or `queen-draw4` to a different target. The separate VP20 override is restricted to the targeted player's seat in R3B.1.

This task found the existing executable mappings **already correct**. The added Python test checks the canonical identities, both launchers' mappings, mismatched CLI rejection, one-sided VP20 separation, and legal target inclusion. The additional Kotlin test resolves all three real CSV cards and checks seat-specific effective overrides. Neither test renames a historical output archive or alters persisted experiment fingerprints.

Commands from the checkout root:

```bash
python3 -m unittest discover -s bin/tests -p 'test_a06_research_control_identity.py' -v
./gradlew test --tests '*A06ResearchControlIdentityTest'
python3 bin/a03_verify_boundaries.py
```

When repeating R3B/R3B.1 after the scoring recalibration, treat these as *instrument sensitivity tests*, not evidence by themselves that all 36 Plants are balanced. Preserve paired seeded matchups and actual observed effect-output telemetry.
