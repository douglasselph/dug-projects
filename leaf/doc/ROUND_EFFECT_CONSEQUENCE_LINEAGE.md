# Round Effect Consequence Lineage

Research telemetry tracks downstream consequences for four Cultivation Round effects without changing gameplay decisions or RNG:

- `MULCH_DIE_FROM_HAND`
- `UPGRADE_DIE_AND_USE_NOW` (current Compost)
- `GAIN_SUNLIGHT_TOKEN`
- `GAIN_WATER_TOKEN`

The lineage begins only when the effect came from a Round card. Identical shared tokens remain ordinary gameplay objects; research uses FIFO origin queues so a later Water/Sunlight spend can be attributed to a Round acquisition without adding identity to the physical rule model. Mulch is matched by stored die size, which is the only identity the current Mulch token preserves.

## What is followed

**Mulch** records the sacrificed die's sides/face, whether the stored Mulch was later used, the rerolled die, Battle placement, winning-Strike association, individually winner-decisive/wound-decisive status, and associated Battle VP.

**Compost** records the upgraded source die, the rolled replacement created by `UPGRADE_DIE_AND_USE_NOW`, and follows that replacement if it later reaches Battle.

**Sunlight** records Round acquisition, later spend, the funded Main Action (Draw / Plant / Round Effect), funded Plant identity where applicable, a Sunlight-funded Draw die, and immediate die-value effects produced while the funded action executes.

**Water** records Round acquisition and later use as Reroll or Refresh. Reroll records before/after value and follows the affected die in Battle. Refresh records how many face-down Plants and spent Butterflies were restored at that use.

## Interpretation

This is provenance, not a general causal graph. `winner-decisive` and `wound-decisive` reuse the existing Strike contribution ledger and are strong local diagnostics. `associated Battle VP` means a traced die/effect participated in a Strike that awarded that VP; it must not be read as proof that the Round effect alone caused the final game result.

Both `evaluate_cultivation_main_policy` and the policy-interaction evaluator now print a `ROUND EFFECT DOWNSTREAM CONSEQUENCES` section so fresh player-count-specific Cultivation Main studies can answer not only which Round effects are selected, but what they subsequently produce.
