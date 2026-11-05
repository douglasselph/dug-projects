# V38-5 — Special Battle Interaction Audit

**Patch authored:** November 5, 2026  
**Authority:** `doc/rules/Leaf_and_Let_Die_v38-01.pdf`, pp. 15–17, 19–21; official Plant and Wisp CSVs.  
**Status:** focused tests authored; **not yet run on Ubuntu**. The passing V38-4 full-game regression does not guarantee these rare interactions.

## Why this increment

The official rule sequence is High → Middle → Low; each Strike does **Engage → Resolve → Doom → Cleanup**. The lowest *value group* dies in each row, even if it comprises only one die; surviving dice and committed Critters are reclaimed immediately; Refresh happens once at the end. Wisp's Resolve may finish another open row and thus skip that row's normal turn. All remaining dice in open rows retain their current controller/Hand ownership.

## Deterministic coverage

| Interaction | Specific test | What is asserted |
| --- | --- | --- |
| Pollen Theft followed by v38 Doom | `V38CrossEffectStrikeFinishTest.Pollen Theft changes die controller before per Strike Doom and cleanup` | Actual same-sized cross-player swap keeps faces, changes *both* grid and exact Hand owner; Doom removes the new owner's lowest die; future row remains unchanged. |
| Root & Scoot followed by v38 Doom | `V38CrossEffectStrikeFinishTest.Root and Scoot withdrawal prevents Wound but opponent die still faces single-die Doom` | Raise before withdrawal; withdrawn player excluded from Wound/VP total; even the sole remaining die is Doomed; other row preserved. |
| Wispquake after true completed-row cleanup | `WispquakeEffectTest.battleWispquakeAfterRealStrikeCleanupOnlyRerollsRemainingHandDice` | Finished-row dice are no longer in Hand; Wispquake preserves the chosen actor die and rerolls only remaining open-row opponents; frozen resolution unchanged. |
| Multiple Wisp's Resolve during a single capped turn | `ThreeEngagementBattleTest.two Resolve Wisps finish two future Strikes in one capped Support turn` | Two actual Wisp supports finish Middle and Low during High; both rows are skipped when reached, with unique resolution/Doom/cleanup, while the two-Support cap is respected. |
| Sunlight extra Main during Engage | `ThreeEngagementBattleTest.Sunlight funds extra Main inside one Support slot without spending normal Main` | An extra Draw from one Sunlight Support is logged; the player may also take the normal Main in the same Strike; only one ordinary token is consumed. |
| Single Wisp's Resolve / early current row / frozen snapshots | Existing `ThreeEngagementBattleTest` and `WispLastWordEffectTest` | Preemption and mid-action finish, preservation of completed-row results, and no double settlement. |
| General locked-row mutation | Existing `ThreeEngagementBattleTest`, `DieValueBattleRowEffectsTest`, `CrossPlayerEffectHandlerTest` | Closed-row mutation rejected, while legal open-row targeting and exact ownership remain possible. |
| Full round cleanup / final Refresh and regression | V38-4 six-case matrix, `BattleCleanupCoordinator` tests | Complete-game cleanup count and deterministic replay across both rulesets; does not prove every rare card effect. |

The first five rows above were added in this patch. The remaining rows are existing coverage and should be rerun after implementation changes.

## Run on Ubuntu

From the `leaf` root:

```bash
bin/verify_v38_special_interactions
```

The helper executes the focused Kotlin unit and simulation suites, validates JUnit XML for the named classes, and archives test logs, XML, fingerprints, Gradle exits and an assessment in a **single `.tgz` under `~/Downloads`**. Upload the file printed after `UPLOAD THIS FILE:`. The helper returns nonzero on missing, failed or skipped evidence. Tests are scoped, not a substitute for full `test integrationTest simulationTest` after any production change.

## Remaining acceptance and limits

- **Must run the newly added tests on Ubuntu** before describing these paths as verified. If a test exposes a discrepancy, fix the smallest underlying rules error and repeat focused + full regressions.
- Review less common multi-card chains, especially one effect earning an immediate Wisp during a Support turn, Wounds that disable a later-row Plant activation, and targets whose row becomes closed while a nested effect executes. These are not proven merely by the new fixtures.
- The audit tests **mechanical compliance**, not player decision quality, card pricing, or card-effect balance. Do not advance v38 experimental research solely on the basis of these unit tests without A1 ruleset-aware runner/provenance work.
- No official card text, rules or default-policy behavior was changed by this increment.
