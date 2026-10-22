# Task 5B.4 — Petal acquisition projections

Research-only Human Baseline change. Does not alter card rules, selection legality, CSV configurations, AI weights, or RNG.

## Shared projection

`PetalAcquisitionProjection` recognizes the historical code plus P1–P4. It enumerates publicly visible candidate D4s from the Graft Bed and eligible opponent Hand/Discard zones; Hand-destination candidates require Battle placement capacity. The production rules engine remains the ultimate authority on legality.

- One new owned D4: end-game scoring contribution (10 scoring points) and bounded future-cycle availability (up to 10 points from remaining rounds).
- Steal: opponent permanently loses an owned D4 (10 points of opponent denial). If stolen from the active Cultivation Hand, also value the purchase tiers that become inaccessible. If stolen from the active Battle Grid, project that known die's removal in its actual row and evaluate the Battle swing; never award an additional generic Hand-denial value.
- Acquired to Discard: no immediate purchase/Strike benefit.
- Acquired to Hand: in Cultivation add an actual stolen die face or the Grove D4's predictive mean (2.5) and count currently accessible purchase-tier improvements. In Battle take the best legal placement using the existing BattleDiePlacementAnalyzer. The acquired die is not rolled by this evaluator.
- Optional cycle: evaluate the positive net improvement from replacing each available D4 with an arithmetic-mean future Draw. If a just-acquired Hand D4 is cycled, count only the *incremental* advantage of the replacement over keeping that acquired D4, avoiding double credit.

## Boundaries and later work

The scoring is explicitly a *baseline priority*, not a forecast of final VP. It preserves the existing score-unit conventions and uses no mechanical RNG. The next Draw follows the same public supply/refill approximation used by existing Human Baseline helpers (not a hidden future die). Future D4 availability is a bounded proxy pending full multi-round simulation. Purchase tier crossing uses currently available costs, not an exact multi-item Buy optimizer. Battle cycling can be improved by a later conditional placement planner. Existing `chooseDice` still uses its expected-reroll ranking, rather than a complete forecast of every combination and placement; integrating that is a possible follow-up before freezing the Human control. These approximations are not concealed as exact projections.

No simulation experiment should yet be taken as authoritative without passing the test suite and verifying the predicted-versus-realized scenarios.
