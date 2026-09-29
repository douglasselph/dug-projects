# Human Baseline shared context/value helpers

This checkpoint introduces deterministic, player-visible observations needed by later Human Baseline calibration. It intentionally does **not** attach new priority points to these observations and therefore does not change Human Baseline choices.

## Public next-phase awareness

`GameProgressView.upcomingRoundTypes` exposes only the public phase pattern of unrevealed rounds. It does not expose unrevealed Round-card identities or effects. `PhaseProximity` answers next phase and approximate rounds until Battle.

## Creature Refresh Value

`CreatureRefreshValue` identifies immediate refresh (one face-up Plant remains), the shallow two-Main-Action opportunity (two remain), what cards would be restored, whether Battle is next, and the existing Cultivation/Battle base totals of the restored cards. The totals are observations, not approved refresh bonuses.

## Phase-Preservation Value

`PhasePreservationValue` records whether Battle is next, the selected card's current Cultivation/Battle bases, their difference, and whether immediate/two-action refresh changes the preservation context. It deliberately does not decide a preservation penalty yet.

## Future Dice Availability

`FutureDiceAvailability` estimates how many visible draws stand between a discarded die and natural availability: remaining Supply dice plus lower-sided/equal-earlier Discard dice. It also reports Battle proximity and the visible upcoming Supply-side sequence. It does not predict rolls or hidden randomness.

## Dice-Pool Quality

`DicePoolQuality` reports owned cycling dice, D4/D6 counts, weak-die fraction, prepared Mulch count, and visible upcoming Supply sides. The proposed occasional human choice to pursue weak-die cleanup is intentionally not randomized here; that belongs in the strategy layer after calibration.

## Purchase Opportunity Cost

`PurchaseOpportunityCost` delegates to the existing `PurchaseThresholdHeuristics` and exposes before/after power plus crossed Buy tiers. This avoids a second definition of purchasing opportunity.

## Calibration boundary

These helpers provide shared facts for later Mulch, Plant timing, Refresh, preservation, and Forget-Me-Not work. No new percentages, priority bonuses, or card-specific behavior are introduced by this checkpoint.
