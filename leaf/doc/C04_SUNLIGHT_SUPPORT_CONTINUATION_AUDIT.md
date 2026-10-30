# C04 — Sunlight and Support continuation audit (2026-10-30)

## Production finding

`SunlightBattleSpendingValue.observe()` applies a minimum preservation cost of 5 points. `score()` previously reconstructed raw cost adjustments without applying the minimum, so an actor with many Sunlights in the final Battle could receive an unjustified extra preference bonus. The real choice score now subtracts exactly the preservation cost from `observe()`. This changes only Human Baseline priority scoring, not game rules, token spending, or turn order.

## Tests

- `SunlightBattleSpendingValueTest`: held eight Sunlights in final Battle; checks cost floor and actual priority score equals observed net value.
- `BattleActionCoordinatorTest`: Sunlight-funded Main generates Worm Supports, **and** the original Final Main is still offered in the regenerated choice list; previously checked generated Worm choices and eventual Final, now asserts simultaneous availability after funding.
- Existing positive tactical Worm and multi-Sunlight tests remain in place.

## Remaining work

The baseline is still a shallow continuation model, not a full search of opponent replies or all possible next rolls. Further mixed candidate tests and Kotlin/simulation execution are required before closing C04.
