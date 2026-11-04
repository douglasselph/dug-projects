# v38-01 Battle Engine: Strike Doom, Cleanup, and Wisp's Resolve

**Implementation checkpoint 1 — 4 November 2026**  
**Source:** `leaf(20261104-193030).tgz`  
**Rule authority:** `doc/rules/Leaf_and_Let_Die_v38-01.pdf`, pages 16–17 and 21.

## Scope delivered

- `THREE_ENGAGEMENTS` now performs **Resolve → Doom → Cleanup separately for each Strike**, High → Middle → Low.
- Doom trashes *exactly the dice tied for the lowest face value of the current row*, including when there is only one; D4s return to the Graft Bed, other die sizes leave play.
- Surviving dice go to their current owner's Dice Discard Bin, committed Critters return to the Grove **after each row**. Physical cleared dice leave the player's Battle Hand.
- Each resolved row freezes its scoring/effective-Critter snapshot before Doom; closed rows may be physically reclaimed only in a narrowly scoped operation. All later targeting/actions exclude them.
- **Final Refresh, pending Mulch normalization, and temporary Critter value resets happen once, after all three Strikes**, not between them.
- Wisp's Resolve is now a legal Battle Support in `THREE_ENGAGEMENTS`, with the same Resolve → Doom → Cleanup as a normal Strike. Resolving the active row ends that Engagement immediately. Resolving a future row skips its ordinary turn sequence when reached; no duplicate award/Doom/cleanup.
- `data/Wisp_Cards.csv` and `GameEffectConverter` recognize the official v38 Wisp's Resolve printed effect. The old text remains a deliberate converter alias for historical data.
- Existing global `DoomResolver.execute` and `BattleCleanupCoordinator.execute` remain intact for the current `CANONICAL` (legacy whole-Battle) path. Legacy Wisp execution also retains its old behavior. Experimental A/B cohorts that explicitly omit Resolve can still do so.
- Battle pacing statistics now count Critters returned by **all** per-row cleanups, not only the last Chronicle cleanup events. Wisp-preempted rows no longer assume they had a last actor.

## Not part of this checkpoint

This patch does **not** yet switch the default or rename `CANONICAL`. `data/research/resync/game-rules-current.properties` still selects `CANONICAL` in the supplied archive. To exercise the new behavior in whole-game simulations **with current code**, set `battle.structure=THREE_ENGAGEMENTS`. The following migration checkpoint should make that the default and rename the old enum to `LEGACY_WHOLE_BATTLE` (including research flags, tests, and historical compatibility).

The complete v38 Plant CSV refresh, remaining card text converter mappings, and Compost unavailability fallback are also **not in this patch**.

The early A/B Battle Pacing experiments intentionally excluded Wisp's Resolve in both arms. Those experiments should be labeled historical v37-era comparisons, and not silently treated as v38-compliant research data.

## Run focused tests on Ubuntu

From the top-level `leaf` repository after applying the ZIP:

```bash
./gradlew test \
  --tests '*DoomResolverTest' \
  --tests '*BattleCleanupCoordinatorTest' \
  --tests '*WispLastWordEffectTest' \
  --tests '*WispCardRegistryTest'

./gradlew simulationTest \
  --tests '*ThreeEngagementBattleTest'
```

Once those pass, run the broader regression gate:

```bash
./gradlew test integrationTest simulationTest
```

**Verification limit in the patch authoring environment:** the Gradle wrapper attempted to fetch Gradle 8.13 from services.gradle.org and failed due to unavailable DNS/network. Therefore Kotlin compilation and tests are **not claimed to have passed**. The source/CSV-level consistency check and ZIP integrity check were performed, but you must run these commands locally before treating this as verified engine behavior.

## Particular edge cases to retain as follow-up regression tests

1. Pollen Theft changes both exact die owner and row before Doom/cleanup; returned dice must follow the *current* owner.
2. Root & Scoot withdraws a player from a Strike before its resolution; no phantom VP/Wounds, and withdrawn dice should already be discarded.
3. A Wisp finishing a row in the middle of a Main/Support chain stops that current Engagement without granting extra turns.
4. Multiple Wisp Resolves in one Battle must only target still-open rows and must never double-score.
5. Sunlight-funded extra Main Actions still consume one of the current Strike's two Support opportunities, while normal Main Action tokens stay limited to two for the whole Battle.
6. Wispquake and all die effects must respect already-closed rows; cleared dice are no longer in Hand.
7. Compare `THREE_ENGAGEMENTS` mechanical telemetry under 2, 3, and 4 players to ensure timing and next-strike action generation remain valid.
