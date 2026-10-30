# B02 — Sunlight Round-card runtime trace and resync proof

Scope: **read/trace/verify only**. No canonical effect, card cost, game rule, research override, trained weight or learned policy was changed. Archive basis: `leaf(20261026-024253).tgz`.

## The three shipped Cultivation Round cards

| CSV `name` | Slot 1 title/text | Slot 2 | Resolved slot 1 |
| --- | --- | --- | --- |
| `Resource_Sunlight_Compost` | Sunlight / Gain Sunlight token | Compost | `GAIN_SUNLIGHT_TOKEN` |
| `Resource_Sunlight_Mulch` | Sunlight / Gain Sunlight token | Mulch | `GAIN_SUNLIGHT_TOKEN` |
| `Resource_Sunlight_Water` | Sunlight / Gain Sunlight token | Water | `GAIN_SUNLIGHT_TOKEN` |

Actual source: `data/Turn_Cards.csv`. These are **effect slot FIRST** regardless of player decision order. The obsolete `RAISE_DIE_PLUS_3` is not the canonical meaning of Sunlight.

## Actual execution chain

1. CSV `data/Turn_Cards.csv` → `RoundCardRegistry.loadFromCsv` → `parseEffect` → `GameEffectConverter("Gain Sunlight token")` maps to `GameEffect.GAIN_SUNLIGHT_TOKEN`.
2. `RoundCardManager.loadCards(registry)` saves one definition per card; `RoundDeck` deals the selected canonical `RoundCard`, respecting its `quantity`. In the regression scenario `RoundDeck.setupExact` fixes each selected Round card deterministically.
3. Ordinary gameplay: `GameConfig.roundValues` defaults to `RoundValueResolver.CANONICAL`, which returns the effect stored on the card.
4. Research: `RoundExperimentResearchConfig.resolve(roundPath, canonicalCards)` uses `RoundExperimentConfigLoader` to parse the explicit `round_card,effect_slot,effect` CSV; `RoundExperimentConfig.effectFor` substitutes **only** the named card/slot, falling back to `CANONICAL` for all others. `round-resync-current.csv` is presently header-only; it makes **zero** interventions.
5. `RoundCoordinator.revealNext` reads `game.config.roundValues.cardFor(card)` and records the **effective** effects in `Moment.RoundRevealed`. It returns the original card in `RoundReveal`; `executeRevealed` creates the effective card again and gives it to `CultivationRound.execute` (no mutation of the catalog).
6. `CultivationRound.executeRound` → `CultivationBuildCoordinator.executeActions`. For a legal chosen `CultivationMainAction.RoundEffect1`, the Build coordinator creates a `GameEffectRequest` with `RoundEffectSlot.FIRST`, effective `roundCard.firstEffect.effect`, and `GameEffectPhase.CULTIVATION`.
7. `DefaultGameEffectExecutor` delegates resource effects to `ResourceEffectHandler`. `canExecute(GAIN_SUNLIGHT_TOKEN)` tests `game.grove.tokens.hasSunlight`; `execute` calls `SunlightTokenResolver.gain(game, actor)`.
8. `SunlightTokenResolver.gain` removes one shared `Token.SUNLIGHT` from the Grove, adds it to the acting player's tokens, and records `Moment.SunlightTokenChanged(..., GAINED)`. `ResourceEffectHandler` separately records research asset provenance. **No die Raise operation occurs.**
9. `DecisionContextFactory.create` also passes the effective Round card into the player-facing decision context. This aligns what the decision sees with what the engine executes.

## Configurations explicitly exercised by `B02SunlightRoundRuntimeTest`

| Configuration | Override file | Expected Sunlight slot |
| --- | --- | --- |
| Ordinary game | none; canonical resolver | Gain Sunlight token |
| Current research resync | `data/research/resync/round-resync-current.csv` (header only) | Canonical fallback: Gain Sunlight token |
| Explicit Sunlight research override | `data/research/round-overrides/sunlight-token.csv` (three entries) | Explicit Gain Sunlight token |
| Historical pre-official research snapshot | `data/research/resync/round-resync-pre-official-20261023.csv` | Explicit Gain Sunlight token; independent Compost overrides in slot 2 |

The new simulation test runs a *real scripted Cultivation round* for each of the 3 cards × 4 configurations (12 runs), using a legal nine-card Grove and `RoundCoordinator → CultivationRound → CultivationBuildCoordinator → DefaultGameEffectExecutor`. It asserts the revealed effective effect, the two scripted players' slot-1 Main Actions, two GAINED events, two actual Sunlight tokens transferred from the Grove, unchanged fixed die values, absence of die-value changes, and unchanged canonical card definitions. A separate test verifies slot targeting and the historical second-slot overrides.

**Important boundary:** The test forces the legal Round action via a deterministic `CultivationStrategy`. It proves *what happens when that action is used*, not that the current Human Baseline naturally chooses Sunlight at a particular frequency or score. Human choice valuation belongs to later calibration tasks.

## Limitations and follow-up

- `RAISE_DIE_PLUS_3` remains executable when explicitly requested through legacy paths; this is **not** canonical Sunlight behavior and is the separate B03 cleanup concern. Do not delete historical frozen-weight features or experiment artifacts in B02.
- The ordinary and resync checks depend on the repository's shipped CSVs. A user-supplied malicious/alternative override file could intentionally select Raise +3 until that capability is explicitly blocked.
- A03 boundary checker must remain green. No production code or research data edits are part of this patch.

## Local verification on Ubuntu

```bash
./gradlew simulationTest --tests '*B02SunlightRoundRuntimeTest'
python3 bin/a03_verify_boundaries.py
python3 -m unittest discover -s bin/tests -p 'test_*.py' -v
```
