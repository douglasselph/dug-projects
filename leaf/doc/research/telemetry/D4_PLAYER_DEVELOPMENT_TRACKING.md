# D4 — Per-player exact Plant and dice development

D4 is an **opt-in** lifecycle recorder. Create one `PlayerDevelopmentTracker.attach(player)` per game player **after initial setup and before the first game action**. Retain the tracker until the end, then read `snapshot()`. No training or evaluation-runner integration occurs in D4; that is a later task.

- Plant acquisitions use each `CreatureCardId` (a *physical copy*), plus `PlantCard.title` (display name), its transient CSV ID, cost, type, initial position and side. Snips remain in history (`SNIPPED`), surviving Plants are `GRAFTED`. Copies are **not** collapsed by name.
- Die history uses `PlayerDice.stableDieId`, original sides, latest sides, fate (`OWNED`, `DOOM`, `TRASH`), and upgrade count. Intra-player Supply/Hand/Discard moves aren't acquisitions. An upgrade retains the die's lineage: it is **one investment**, not two acquired dice.
- `DoomResolver` tags its die removals `DOOM`; other calls to `TrashResolver` tag `TRASH`.
- The recorder is a read-only observer and is absent from normal games. Do not attach multiple trackers to the same player simultaneously (callbacks would be replaced).
- Initial dice already owned at attachment are captured. *Past* activity cannot be reconstructed if a tracker is attached midgame.
- Die exchanges between players, nonstandard removal paths, and creature `clear()` are not attributed as acquisitions/removals by this checkpoint; investigate before relying on the ledger for studies using such effects. In particular, this is not yet a verified complete audit of every transfer. D5 should verify these paths and extend exact removal provenance.
- Position is the graft location, not a continuously reconstructed rendered Creature. Final geometry will require comparing the surviving entries against the final Creature state.
- D4 does not attribute final VP, wins, Battle rows, or decision disagreements. Later tasks join these with outcomes and report results by card name and player count, **including zero-acquisition cards**.

## Smoke test

`./gradlew test --tests '*PlayerDevelopmentTrackerTest'`
