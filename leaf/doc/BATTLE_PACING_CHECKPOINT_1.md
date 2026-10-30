# Battle pacing — Checkpoint 1: ordering + paired real Battle openings

**Status:** experimental research scaffolding only. No canonical Battle scheduling, card effect, trained policy, or core game state was altered. This is not an A/B outcome test.

## Rules decisions supplied October 30, 2026

1. Candidate B is **three Engagements**, High → Middle → Low. The current v37.03 two-normal-Main-token economy remains unchanged. Up to two Supports per Engagement, including Sunlight as a Support, are for Checkpoint 2.
2. **Once a Strike is resolved, no further changes can ever be applied to that Strike, including its dice, Critters or outcome.** This includes cross-row/mass effects and Wispquake. Resolved dice remain in place through Doom. This is the uniform rule, no exceptions.
3. **Wisp's Resolve / Last Word** is **excluded from Candidate B's experimental normal Wisp deck**. A card/deck removal option is desired but is not implemented in this checkpoint. That deck filtering is deliberately **not** applied by Checkpoint 1, because the two prefixes must be identical and Candidate B's action phase does not yet exist. Checkpoint 2 must apply a documented, exact experimental Wisp-deck selection while preserving a valid shared pre-Battle state. Do not delete or change the canonical card.
4. No new special markers are needed.

## Source changes

- `src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/EngagementOrderSelector.kt`: experimental-only deterministic selector; frozen ordering evidence and trace. Never uses random numbers and never mutates a Game or BattleState.
- `src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/PairedBattleOpeningReplay.kt`: replay two **independent** fresh `GameFactory` games through the **real** `RoundCoordinator`, then the production `BattleRound.executeOpeningDraw` and `executeRankAndPlace` steps. Uses the same seeds, policies, Round setup and Grove catalog. Produces a copied per-player / shared-resources / Round Card / future deck / Wisp deck / dice zone / three-row fingerprint, and checks that both branches agree **before any actions**. Retains both independent live Game/BattleState pairs for subsequent experimentation.
- `src/simulationTest/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/EngagementOrderSelectorTest.kt`: legal live placements, 2/3/4 players, empty rows, column tiebreak, physical clockwise seating, frozen current order, future order recalculation, invalid participants / closed row.
- `src/simulationTest/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/PairedBattleOpeningReplayTest.kt`: real-engine paired 2-player early, 3-player middle, and 4-player final Battle openings under 3/2/2. Confirms independently instantiated graphs match and that later mutation cannot change the captured fingerprint. Emits reproducibility and ordering evidence to the test console.

### Observable example evidence

```
PAIRED_OPENING,seed=...,battle=1,round=4,players=2,sha256=...,matched=true
ENGAGEMENT_ORDER,row=TOP,position=1,player=...,starter=...,column=...,highestShowing=...
...
```

The trace is metadata about prospective action opportunities, **not actual player actions**, so it is not a balance result or a tabletop-time estimate. Column is 0-based; position is 1-based. Starter is locked only within that row.

## Verification commands (ZIP file in ~/Downloads)

```bash
unzip -o ~/Downloads/leaf_battle_pacing_checkpoint1.zip -d ~/src/dug-projects/leaf
cd ~/src/dug-projects/leaf
python3 bin/a03_verify_boundaries.py
./gradlew simulationTest --tests '*EngagementOrderSelectorTest' --tests '*PairedBattleOpeningReplayTest'
```

Replace `~/src/dug-projects/leaf` if your checkout is elsewhere. If Gradle reports unknown test options for this custom task, use `./gradlew simulationTest` to run all simulation tests; unlike the main `test` task, this is the dedicated simulation source set.

### A03 scope

Only `src/simulation/**`, `src/simulationTest/**` and `doc/**` were changed. **No `src/main/**`, `data/**`, `bin/**` or build files were changed.** The current `bin/a03_verify_boundaries.py` explicitly hashes `src/main`, `data`, `bin`, `gradle` and build files, not the research source set. Therefore a PASS is expected but does not certify the new simulation files. Check their exact ZIP diff independently.

## Known limits and next Checkpoint

- Two runs use the same seeds and replay actual legal histories. Comparing snapshots does not prove the hidden RNG cursor and/or future divergent event streams match. Actual A/B experiments must separate seed pairing from event-keyed/percentile matching and report divergence.
- No game clone, scheduler injection into canonical rules, row locking enforcement, Wisp removal, actual Candidate B turn, VP/Wound calculation, Doom or full A/B Battle execution is implemented here.
- The fingerprint copies deterministic, decision-relevant physical state; it does not claim to serialize the entire internal engine object graph, hidden RNG state, decision-policy internals or the Chronicle.
- Checkpoint 2: experimental scheduler action-window execution, 2 normal Main tokens, up to 2 Supports per row, real legal actions, Sunlight, immediate Wounds, strict row-lock audit for **every** effect, resolved-dice participation in Doom, and Candidate B Wisp exclusion. Gate before reporting win-lift statistics or time estimates.
