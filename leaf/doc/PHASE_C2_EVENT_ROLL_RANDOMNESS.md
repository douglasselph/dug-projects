# Phase C2 — Opt-in event-keyed die rolls

Set `GameConfig(eventBasedDieRolls = true, seed = <fixed non-null long>, ...)` in a simulation. Default is `false` and preserves historical sequential RNG behavior.

Under opt-in mode, **RollResolver-mediated** ordinary draws, rerolls, and die rolls in Cultivation, Battle, and effect handlers receive a face from `EventRollRandomness` rather than consuming the shared mechanical randomizer. The key contains `mechanicalSeed`, `roundNumber`, `playerId`, `player-local physical die ID`, and `per-round occurrence of that die's roll`. Face generation is deterministic and uniform across each die's faces. Changes to other players' die rolls or to other dice do not advance a matching die's outcome. The optional mechanical intervention still runs *after* the natural roll.

Starting die identities are assigned from supply/hand/discard positions. Later dice receive player-local IDs on first registration. Upgrades replace physical dice, so their identity currently changes. Cross-player swaps retain the objects but a receiving player allocates a local ID. Thus stable identity is strongest for unchanged starting dice; acquisitions/upgrades may cause identities to drift across divergent continuations. A fixed key for a die upgraded from D4 to D6 is not yet guaranteed. No percentile coupling is used: the face depends on the number of sides.

This is **not** yet comprehensive event matching. Non-`RollResolver` direct `die.roll()` calls, any other direct random draws, Wisp deck draws, round-card shuffles, ordering tie-breaks, and special-effect randomness retain historical sequential behavior. We intentionally leave those for a later RNG migration. Mechanical RNG consumption can still diverge from such events and different non-die game choices. `EventRollRandomness` is game-local and never shared between games.

Recommended checks: unit test `EventRollRandomnessTest`, `RollResolverTest`, and normal game smoke tests with `eventBasedDieRolls=true` then compare same-seed game replays. Do **not** use this opt-in as proof of identical counterfactual outcomes for all mechanics.
