# Shared Token Economy Research

`evaluate_buy_policy` now reports compact whole-game physical-supply accounting for the finite shared Grove resources:

- Water
- Sunlight
- Mulch
- Bee
- Worm
- Butterfly

The production inventories expose an observation-only seam. Player inventories use a no-op observer; only Grove inventories feed the game-local `SharedTokenEconomyTracker`. Gameplay decisions never consult the tracker, and the tracker consumes no RNG.

## Meanings

A **gain attempt** is an actual attempt to remove a physical component from the Grove inventory. A successful withdrawal is a **successful gain**. A failed withdrawal while the tracked Grove supply is zero is a **failed empty-Grove gain**.

Most gameplay legality checks prevent an unavailable resource from reaching the withdrawal call. Therefore failed-empty gains are deliberately a narrow physical-transfer measure, not a count of every strategic opportunity blocked by scarcity. Round-effect legal/illegal opportunity reporting should be used for that later decision-level question.

A **return to Grove** is a physical component recycled to the shared supply after spending, use, payment, or cleanup. For this first generic supply report, `spends/uses` uses the same physical-recycle count. This is appropriate for supply-cycle analysis but is not a complete semantic use count for resources such as Butterflies, which can be flipped/used without returning to the Grove.

`maximum outside Grove` is derived from the lowest Grove supply observed, so it also captures physical components temporarily committed outside player inventories, such as Battle critters.

## Source attribution

The generic inventory observer intentionally does not infer acquisition source. Existing typed Chronicle events already retain source information for several mechanisms, and source-specific attribution can be layered on later where it is scientifically useful. Keeping the physical-supply observer source-agnostic avoids creating a generalized causal graph inside component containers.
