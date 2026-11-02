# v37.03 die-Trash fidelity correction

## Authoritative rule

Rulebook v37.03, page 18, Step 7 — Doom: **Trashed D4s return to the D4 space of the Graft Bed; Trashed dice of other sizes leave the game.** Page 5 states the same distinction for *replaced* dice during Upgrade.

## Audited live paths (source archive: leaf(20261030-184541).tgz)

All gameplay **die-Trash** call sites in `src/main/kotlin` use `game/operation/TrashResolver.trashDieFromHand`:

- `battle/DoomResolver`: removes the exact Battle Grid placement and then Trashes the corresponding Player Hand die using the central resolver (`reason = "DOOM"`).
- `effect/special/PetalToDie4Effect`: when the Trash D4 branch is chosen, removes its Battle Grid placement if needed and calls the same central resolver (`reason = "TRASH"`).

The Battle Grid is a location map, not an independent owner. A Battle die remains in `PlayerDice.hand`, making both routes compatible with the same resolver. No additional die-Trash route was found in `src/main/kotlin` at the time of this audit.

**Distinct mechanics:** Upgrade (`UpgradeResolver`) replaces a die, returning D4s and removing larger originals according to its separate rule. Root & Scoot, drawn-die recycling, end-of-round Cleanup, Mulch storage, and dice transfers are not Trash. These deliberately do not call `TrashResolver`.

## Central invariant

`TrashResolver.trashDieFromHand` validates identity, removes exactly the specified die from the Hand, records its owner-removal provenance, then:

- `D4`: calls `game.grove.graftBed.returnD4()` and emits `TrashDestination.GRAFT_BED`.
- `D6/D8/D10/D12/D20`: does not increase Grove supply and emits `TrashDestination.OUT_OF_GAME`.

`DoomResolver` reports `returnedToGraftBed` from that shared resolution. Both scripted callers and the Chronicle therefore expose the same result.

## Focused verification

```bash
./gradlew test --tests '*TrashResolverTest' --tests '*DoomResolverTest' --tests '*PetalToDie4EffectTest'
./gradlew integrationTest --tests '*BattleDoomSanityTest'
python3 bin/a03_verify_boundaries.py
```

The ZIP also includes a reviewed A03 manifest update for the **three modified protected source paths only** (`TrashResolver.kt`, `DoomResolver.kt`, `Moment.kt`). The original archive was A03 PASS. Re-run the unqualified A03 guard after extracting the ZIP.

**Local execution caveat:** The offline portable Gradle cache could initialize Gradle, but `:compileKotlin` failed to resolve uncached Compose and other dependency JARs. Unit/integration tests are provided but were **not executed** locally. No simulation/balance statistics are claimed.
