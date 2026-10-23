# D5 — Development provenance for owner-changing assets

D5 extends D4's opt-in `PlayerDevelopmentTracker`. It changes **only research callbacks**; Buy, Battle, dice values, effects and game rules are unchanged. The tracking object must be attached at setup for each player and cannot reconstruct events before attachment.

## Audited source pathways

| Path | Owner history recorded | Notes |
| --- | --- | --- |
| Graft (`Creature.graft`) | named physical Plant copy `GRAFTED` | D4 hook |
| Snip (`Creature.snip`), including wound and Snip effects | physical copy `SNIPPED` | D4 hook |
| Dice gained into Supply, Hand or Discard | new die for owner `OWNED` | D4 hook; between-zone movements are *not* acquisitions |
| Buy payment, Draw, refill, Recall, hand-to-discard | no new acquisition | same owner's existing die |
| Upgrade (`UpgradeResolver`) | stable owner identity, increment `upgrades` | D4 logic; size is updated, not another acquired die |
| Trash / Doom (`TrashResolver`) | `TRASH` / `DOOM` | D4 hook |
| Petal To Die 4 steal (Hand or Discard) | victim `STOLEN`, recipient `OWNED` | D5: removed die loses former owner's tracking ID, gains recipient-local ID |
| Pollen Theft (`swapExactHandDieWith`) | each departing die `SWAPPED_OUT`, each incoming die `OWNED` | D5: each owner ledger retains its historical copy and adds received copy |
| Mulch storing a die (`ResourceEffectHandler`) | `MULCH_STORED` | D5: no longer incorrectly counted as owned at game end |

## Semantics and safeguards

- IDs in `DieDevelopment` identify one player's **ownership episode**, not a globally stable identifier through transfers. A die stolen and subsequently traded again has distinct ledger entries for each owner. The owner ledger's acquisition and removal counts are preserved; it is not a physical die's global genealogy.
- `OWNED` includes starting dice and newly obtained dice, as D4 currently reports. Swaps and steals are acquired by the receiving player but not necessarily *purchased* by that player. Future reports must not equate acquired and bought.
- `MULCH_STORED` is removed from ordinary die zones but may have a pending token representing its future value; do not label it `TRASH` or `DOOM`.
- A non-game `Creature.clear()` / `PlayerDice.clear()` during reset is **not a supported in-game removal action**. These reset functions are used by setup/tests; create new trackers for a new game. Do not attach a D4/D5 tracker to an existing in-progress game and claim its historical ledger is complete.
- The Plant record's graft coordinates represent original attachment position; surviving final topology should be joined with `Creature.cards` at game end.
- D5 does not produce summaries or attach tracking to evaluation runs; those belong to D6–D10.

## Test

`./gradlew test --tests '*PlayerDevelopmentTrackerTest'`

Before accepting a batch as provenance-complete, run additional end-to-end scenarios for Petal To Die 4 steals and Pollen Theft Battle swaps with both players instrumented.
