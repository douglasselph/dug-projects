# C3: shared-percentile event-keyed dice rolls

Enable both `GameConfig(eventBasedDieRolls = true, sharedPercentileDieRolls = true, seed = 720002L, ...)`. Both flags default to false, so existing simulations and original C2 event rolls remain unchanged.

The event key is (mechanical game seed, round, player, player-local stable die ID, per-round occurrence). C3 generates a deterministic uniform fraction `u` in [0,1) from the key **without including die size**. The face is `floor(u * sides) + 1`. The same event can therefore produce, for example, a relatively high face on D4 and on an upgraded D6 without ever producing an invalid face. Each die size retains uniform marginal distribution. This is a counterfactual coupling strategy, not a change to the physical game's rules.

The normal `UpgradeResolver` now transfers the source die's event identity to its replacement when C2 event rolls are enabled (both upgrade-to-Hand and upgrade-to-Discard). This is necessary because upgrades exchange Die objects. A purchased die still gets a newly allocated identity, and its identity can diverge across different histories.

**Limitations:** Matching requires the same player, round, stable die ID, and roll occurrence. An upgrade made in a different round or a skipped roll can still change keys; an upgrade to Discard delays the die's next appearance. Other RNGs (shuffles, Wisps, purchases, special direct Die.roll callers) are not event matched by this patch. Full mechanical replay equivalence is *not* established. The opt-in shared-percentile mapping correlates matched rolls across different die sizes intentionally.

Validation: run `./gradlew test --tests 'dugsolutions.leaf.v35.game.random.EventRollRandomnessTest' --tests 'dugsolutions.leaf.v35.player.dice.PlayerDiceTest'`, then rerun your game replay experiment with both opt-in flags.
