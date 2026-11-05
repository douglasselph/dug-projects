# Leaf & Let Die — Current v38-01 Battle Status

**As of:** November 5, 2026  
**Current rulebook:** [`doc/rules/Leaf_and_Let_Die_v38-01.pdf`](../rules/Leaf_and_Let_Die_v38-01.pdf)  
**Scope:** Implementation status and remaining acceptance, **not** a claim of proven balance or a new experiment proposal.

## Adopted behavior and selectable compatibility

- **Adopted/default:** `THREE_ENGAGEMENTS`. The optional `data/research/resync/game-rules-current.properties` file may select this explicitly; missing file/property also selects it.
- **Historical only:** `LEGACY_WHOLE_BATTLE`, explicitly selected for reproduction of pre-v38 research/tests. The old `CANONICAL` enum value is rejected with a migration hint.
- **Rule sequence:** In High, Middle, Low order, each unresolved Strike has an Engagement and then **Resolve → Doom → Cleanup**. Doom trashes the current row's lowest-valued die or tied group, **including a single die**. Surviving dice and committed Critters are cleared **per row**. Normal final Refresh runs after the Battle, not after every row.
- **Wisp's Resolve:** can immediately finish an unresolved row through the same Resolve → Doom → Cleanup path. An early-finished future row is skipped when reached, without double scoring.
- The previously approved three-Engagement turn/action limits and existing 3/2/2 (7 Cultivation + 3 Battle) round structure are retained.

## Source changes present in the supplied November 4 archive

| Area | Evidence in source | Verification boundary |
| --- | --- | --- |
| Per-Strike resolution, row Doom/cleanup and Wisp | `StrikeFinishCoordinator`, `ThreeEngagementBattle`, `DoomResolver.executeRow`, `BattleCleanupCoordinator.executeRow`, `WispLastWordEffect`, related tests | Implemented in code; special-card and complete-game regression acceptance not yet demonstrated by a supplied test log |
| Default and legacy override | `BattleStructure`, `GameConfig`, `RoundCoordinator`, `game-rules-current.properties` | Basic config behavior checked previously; full 2/3/4-player matrix still needs local execution evidence |
| Official card text / Compost | Updated `data/Cultivation_Cards - Root.csv`, `data/Cultivation_Cards - VF.csv`, `GameEffectConverter`, Wisp source, `UpgradeEffectHandler`, baseline valuation/tests | 15 Plant text changes; 14 converter registrations; focused converter smoke noted in migration document; no general v38 balance proof |
| Both rulesets / all player counts | `V38BattleStructureFullGameRegressionTest` | **PASSED on Ubuntu November 5:** six dynamic cases; full Gradle suites green. One unrelated long-run memory test intentionally skipped. |

The 2/3/4-player deterministic matrix checks a standard ten-round game under each selected Battle structure. Its Mechanical Control players establish **replay correctness**, not competent strategy, comprehensive Wisp/Plant coverage, or statistical fairness. Older passing tests and the October 30 operational-readiness gate predate the rules migration.

## November 5 full-game acceptance evidence

Uploaded verification artifact: `leaf-v38-verification-20261105-135919-2148522.tgz`, checkout SHA `3304fa6f1351376d249ab2be443a44532a8ed3cd`. Both Gradle commands exited 0. JUnit: 6/6 targeted, 1740/1740 unit, 88/88 integration and 242/243 simulation (last is the explicitly opt-in `BaselineLongRunMemoryTest`; no failures). The corrected strict checker verifies that only this exact skipped test is accepted. **V38-4 is closed.**

## Run next on Ubuntu (V38-5 focused interaction acceptance)

```bash
bin/verify_v38_special_interactions
```

This checks focused effect handlers and the actual Three Engagements scheduler, collects JUnit/log/provenance into one `~/Downloads` archive, and prints `UPLOAD THIS FILE:` even on failure. The authored V38-5 tests are not yet certified until the archive is reviewed.

### Re-running the entire regression matrix (if needed)

From the repository root:

```bash
set -o pipefail
./gradlew simulationTest \
  --tests '*V38BattleStructureFullGameRegressionTest' \
  --rerun-tasks |& tee ~/Downloads/leaf-v38-full-game-regression.log
./gradlew test integrationTest simulationTest \
  |& tee ~/Downloads/leaf-v38-all-regressions.log
```

Record actual pass counts and failures with JUnit XML/log paths before closing the gate. See [V38_01_FULL_GAME_DETERMINISM_REGRESSION.md](V38_01_FULL_GAME_DETERMINISM_REGRESSION.md) for expected event-level assertions. Do **not** mark this as passed from the presence of the test class or successful ZIP packaging.

## Open follow-ups

1. **Complete rules fidelity:** focused deterministic fixtures for Wisp's Resolve (including two uses), Wispquake after completed-row cleanup, Pollen Theft then Doom, Root & Scoot then Doom, and Sunlight-funded Main were added November 5. **They still require an Ubuntu run.** See `V38_01_SPECIAL_INTERACTION_AUDIT.md` for covered paths and remaining broad cross-row/card-targeting work. Compare the v38 rulebook and official CSV wordings to actual executable paths.
2. **Runtime architecture:** `THREE_ENGAGEMENTS` currently resides in the `src/simulation/` source set. Main-only runtimes lacking its `BattleStructureProvider` cannot execute the new default; they must not silently use legacy. Decide on a narrowly-scoped move into main only when appropriate.
3. **Experiment provenance:** existing v37 pacing, R3A and card-screen work was performed with historical Battle rules. R3A-based research adapters currently force `LEGACY_WHOLE_BATTLE`. New v38 card/balance studies need ruleset-aware adapters and fingerprints, not silent reuse of legacy outputs.
4. **Design evaluation:** Task 7 fairness/counterfactual package is not fully validated; human pacing estimates are uncalibrated proxies. Re-evaluate order bias, High-Wound cascades, resource stranding, card and Wisp interactions, and actual downtime under v38 before making design claims.

## Provenance / history

- [BATTLE_STRUCTURE_CONFIGURATION.md](BATTLE_STRUCTURE_CONFIGURATION.md) — current configuration and runtime boundary.
- [V38_01_BATTLE_ENGINE_IMPLEMENTATION.md](V38_01_BATTLE_ENGINE_IMPLEMENTATION.md) — scoped earlier checkpoint, including **historical** statement that default was not yet switched at that time.
- [V38_01_CARD_CATALOG_AND_COMPOST_SYNC.md](V38_01_CARD_CATALOG_AND_COMPOST_SYNC.md) — official-data and Compost migration record.
- [BATTLE_PACING_RESEARCH_STATUS.md](BATTLE_PACING_RESEARCH_STATUS.md) — **historical v37 Candidate-B experiment**; its old `CANONICAL`/adoption/Doom statements do not describe v38.
- [TASK7_STATUS.md](TASK7_STATUS.md) — unvalidated Task 7 fairness/counterfactual extension.

The broader mission and research priorities remain governed by root [`README.md`](../../README.md) and [`TASKS.md`](../../TASKS.md). No old pacing research is automatically evidence that the adopted v38 behavior is balanced or improves actual tabletop minutes.
