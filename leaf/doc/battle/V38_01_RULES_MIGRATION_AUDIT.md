# Leaf & Let Die — v38-01 Rules Synchronization Audit

**Source reviewed:** `leaf(20261104-190148).tgz` (no `.git` metadata in archive).  
**New rules:** `Leaf_and_Let_Die_v38-01-small(1).pdf`, 23 pages.  
**New official Plant data:** attached `Cultivation_Cards - Root(6).csv` (12 cards) and `Cultivation_Cards - VF(6).csv` (24 cards).  
**Audit scope:** repository inspection and card text comparison; no Kotlin source modifications or execution tests yet.  
**Status:** MIGRATION NOT YET IMPLEMENTED; this package only installs the v38 rulebook and this checklist.

## Rule-set identity and intended default

- Adopt `THREE_ENGAGEMENTS` as the normal/default Battle structure, including when the properties file is absent or omits the setting.
- Rename the old v37 whole-Battle action sequence + end-of-Battle resolution to **`LEGACY_WHOLE_BATTLE`** (proposed name). Keep its old Doom/cleanup behavior for explicit historical comparison.
- Do **not** rename unrelated `PlantValueResolver.CANONICAL`, `RoundValueResolver.CANONICAL`, or other uses of canonical describing card data.
- `src/main/kotlin/dugsolutions/leaf/v35/game/BattleStructure.kt`: change enum, `load()` absent-file fallback, and the property default.
- `src/main/kotlin/dugsolutions/leaf/v35/game/GameConfig.kt`: ensure normal new Games select the new default.
- `src/main/kotlin/dugsolutions/leaf/v35/game/round/RoundCoordinator.kt`: select the new engine by default and old engine only on explicit `LEGACY_WHOLE_BATTLE`.
- `data/research/resync/game-rules-current.properties`: set `battle.structure=THREE_ENGAGEMENTS` or remove that assignment so code fallback applies. An explicit legacy override should be `battle.structure=LEGACY_WHOLE_BATTLE`.
- `src/simulationTest/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/BattleStructureRoutingTest.kt`: assert absent file, absent key, explicit legacy, explicit v38, invalid values, and simulation-provider availability.
- `src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/ConfiguredSimulationBattle.kt`: currently bridges only the simulation source set; make ordinary simulation entry points consistently use it, and ensure no silently wrong production-only fallback. Decide whether core promotion is warranted only after dependency review.

## Battle mechanics: verified differences

| Feature | Existing experimental THREE_ENGAGEMENTS | v38-01 required |
|---|---|---|
| Rank/place | Existing High/Middle/Low initial dice | Retain |
| Per-row starter | Highest die of current row, leftmost column tie; clockwise | Retain |
| Action budget | Up to 1 normal Main and 2 Supports per row; 2 normal Mains total | Retain |
| Immediate VP/Wounds | Per row, before next Engagement | Retain |
| Doom timing | Once after all three rows | **After EACH row** |
| Doom selection | Old global algorithm: at least 2 dice, second-lowest when needed | **Exactly all dice tied for lowest face value IN THAT ROW, even if one** |
| Dice/Critter cleanup | Once after all rows | **After EACH row** |
| Removed/locked row | Actual dice retained for lock checks | **Row becomes empty/closed; immutable resolution evidence retained** |
| Refresh | End of Battle | **Only after Low Strike / all three rows resolved** |
| Wisp's Resolve | Explicitly excluded from candidate deck and legal Supports | **Included: immediately Resolve → Doom → Cleanup chosen unresolved row; skip it later** |

### Exact implementation locations

- `src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/ThreeEngagementBattle.kt`: replace final global `executeDoom`/`executeCleanup` with row-scoped operations and one final Refresh. Remove mandatory Resolve-free-deck assertion. An already-resolved row (from Wisp) must be skipped rather than submitted to the starter selector.
- `src/main/kotlin/dugsolutions/leaf/v35/battle/DoomResolver.kt`: add row-only, lowest-*value-group*-only entry point. Preserve the existing global >=2-die method for `LEGACY_WHOLE_BATTLE`. Preserve D4 return-to-Graft-Bed semantics.
- `src/main/kotlin/dugsolutions/leaf/v35/game/round/battle/BattleCleanupCoordinator.kt`: separate per-row dice/Critter reclamation from end-of-Battle Plant/Butterfly Refresh, temporary modifier normalization, and round-end checks. Do not prematurely refresh/reset round-wide effects after High/Middle.
- `src/main/kotlin/dugsolutions/leaf/v35/battle/domain/BattleGrid.kt`: current lock protection only allows post-resolution removals after **all** rows have closed (`beginPostBattleRemoval`). Provide tightly scoped removal of a just-resolved row without permitting modification of the result or other locked rows.
- `src/main/kotlin/dugsolutions/leaf/v35/battle/BattleState.kt`: preserve immutable `frozenResolvedRow` snapshots; adjust live-dice/lock assertions for cleared rows.
- `src/main/kotlin/dugsolutions/leaf/v35/effect/special/WispLastWordEffect.kt`: the current effect resolves and clears a row but does **not** Doom it. Reuse exactly the same row-finalization pathway as ordinary Strike resolution. Ensure current/future unresolved Strike may be chosen, no double resolution, and no actions inadvertently continue against a row after immediate finish.
- `src/main/kotlin/dugsolutions/leaf/v35/game/round/battle/BattleActionCoordinator.kt`: `researchLegalSupports` explicitly filters `RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW`. Remove that restriction for v38. Preserve legal targeting of only unresolved rows.
- `src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/battlepacing/ThreeEngagementBattle.kt`: `BattlePacingWispDeck.withoutResolve()` and the execute-time exclusion are historic experimental controls; do **not** use them in ordinary v38 games.
- `data/Wisp_Cards.csv`: `Wisps_Resolve` still describes immediate resolution and returning dice/Critters, omitting Doom. Update printed wording and conversion registration together, per the v38 Wisp Almanac, page 21.

### Normal Battle policy and cross-row interactions

Validate `HumanGuidedEngagementPolicy` (the current simulation adapter) against normal policy selection, ensuring per-row actor order is frozen, normal Main tokens survive to later rows, Sunlight adds a Main through one Support opportunity, and already-cleared dice are **not** still in Battle Dice Hand. Check mandatory swaps, multi-draw, opponent die operations, Butterfly/Water rerolls, Wispquake, Root & Scoot withdrawal, Wounds/Snips, Petal To Die 4, and temporary per-round Bee/Worm effects. Existing `LockedStrikeEvidence` and per-turn research telemetry assume dice remain physically in previously resolved rows; adapt them.

## Additional non-Battle v38 discrepancy

- Rulebook v38 p. 11: if Compost cannot obtain the next die size, the player **may instead trash the chosen die**.
- `src/main/kotlin/dugsolutions/leaf/v35/effect/handler/UpgradeEffectHandler.kt` currently generates `UPGRADE_DIE_FROM_HAND` legal targets only if a normal upgrade is available; it lacks the fallback. Implement the fallback for Compost (not automatically for Root Awakening), including the decision choice, appropriate trash route, and tests.

## Official Plant CSV migration: critical parser issue

The current `data/Cultivation_Cards - Root.csv` differs from the attached Root CSV on 4 effects; `data/Cultivation_Cards - VF.csv` differs from the attached Vine/Flower CSV on 11 effects. All 36 card identities/counts/headers are maintained in the attached files.

Changed titles:

- Roots: Root Double Down, Root Four More, Root & Scoot, Root Recall.
- Vines/Flowers: Vine and Punishment, Vine Yield, Rise and Vine, Vine and Dine, Berry Tasty, Saplink Trellis, Bloom Backbone, Sapping Snapdragon, Transplant Tulip, Bloom Backflip, Bursting Blossom.

Changes primarily remove the display `🎲` symbol; Root Recall corrects a duplicate `dice`; Bursting Blossom changes `Raise any die +1` to `Raise a die +1`. Vine and Punishment normalizes punctuation/spacing.

**14 of those 15 changed effect texts do not match `GameEffectConverter.kt`'s current exact registered strings** (Vine and Punishment alone already matches). Blindly replacing the CSVs now will fail Plant card loading. Make an atomic change of both `data/` CSVs and `src/main/kotlin/dugsolutions/leaf/v35/effect/GameEffectConverter.kt`, with a test asserting all 36 official cards load with non-UNKNOWN effects and unchanged effect semantics except explicitly approved changes.

The changed source text also changes fingerprints used by `bin/plant_question_spec.py`, potentially invalidating example question JSON manifests under `doc/research/experiments/card-balance/questions/`. Recompute current fingerprints only after the data/converter migration; preserve historical manifest identity for historical runs. The resync CSV is a header-only research overlay, not a second official catalog.

## Research and documentation migration

- `bin/plant_question_spec.py` defines `RULES={'CANONICAL','THREE_ENGAGEMENTS'}`; update to the versioned names.
- `bin/plant_question` contains an explicit `CANONICAL` guard for legacy R3A delegation. Update it to require `LEGACY_WHOLE_BATTLE`, retaining old behavior only where the route is truly supported; avoid silently treating v38 as legacy.
- Update two example research question JSONs (`root-four-more.json`, `sapping-snapdragon.json`) and documentation schema, choosing **explicitly** whether each example is a historic v37 or new v38 question.
- `README.md`, `doc/README.md`, `doc/battle/BATTLE_STRUCTURE_CONFIGURATION.md`, and `doc/battle/BATTLE_PACING_RESEARCH_STATUS.md` currently describe v37 as canonical or Candidate B as not adopted. Correct after engine migration. Older `doc/battle/BATTLE_PACING_CHECKPOINT_*` and experiment results are historical; retain with explicit v37 labels rather than rewriting their meaning.
- The A/B pacing runner explicitly invokes the old A and experimental B and excludes Resolve from both decks; its historical outputs are **not** a v38 parity certificate. Version the study design/manifest before using it again under v38. Preserve direct-Java/persistent-JVM, seeds, process isolation, checkpointing, archives, and output schemas whenever possible (see `doc/engineering/SIMULATION_LAUNCH_ARCHITECTURE.md`).
- Keep unrelated uses of `CANONICAL` referring to officially loaded card/round data and `Human Baseline` intact.

## Verification checklist (must pass before any broad balance runs)

1. Absent properties file, blank property, explicit Three Engagements, explicit legacy, unknown property: expected routing or failure.
2. 2/3/4-player legal opening rank/placement; fresh row starter + clockwise order; current Engagement order stays fixed despite die changes.
3. One normal Main per row and two for entire Battle; two Supports per row; Sunlight creates its own extra Main without consuming a normal token, but consumes one Support opportunity.
4. Resolve High/Middle/Low; VP/tie/Wound arithmetic; immediate Flip-or-Snip potentially disables a later Plant activation.
5. Per-row Doom: exactly one lowest die, multiple tied-lowest dice, all dice identical, empty row, D4 return, and non-D4 removal. Verify a low die in another row never affects this row's Doom.
6. Per-row cleanup: surviving dice to owner Discard, Critters return to Grove, just-completed row no longer in Dice Hand, later rows remain untouched and legal, no premature Refresh or round-wide normalization.
7. Resolved-row snapshots and Chronicle remain accurate after grid clearance; later actions cannot target/alter resolved rows.
8. Wisp's Resolve: choose current/future unresolved row, full Resolve/Doom/Cleanup immediately, skip later scheduler entry, exactly one VP/Wound event, reject already-resolved row. Wispquake may reroll only dice that remain in Hand.
9. Roll Reward 1=Critter and 2=Wisp in both round types, including rerolls, respecting exception text and Wisp deck depletion.
10. Compost absent-upgrade fallback may Trash die; Root Awakening remains unchanged.
11. New official Plant CSVs: 36/36 recognized with correct cost/VP/identity; all text converter tests and relevant special-card execution tests pass.
12. Complete-game seed-based smoke for 2/3/4 players with 3/2/2 Round Deck using new default; separate explicit legacy smoke; paired deterministic action/state traces and research manifest ruleset names.
13. Run focused Kotlin unit/integration/simulation regressions and `bash doc/SIMULATION_OPERATIONAL_READINESS.sh` before expensive experiments. Verify launcher architectural invariants before reusing published research datasets.

## Rulebook installation and obsolete files

- The attached ZIP places **`doc/rules/Leaf_and_Let_Die_v38-01.pdf`** here.
- Existing `doc/rules/Leaf_and_Let_Die_v37-03.pdf` belongs in `doc/legacy/` for historical A/B reproducibility. After backing up/committing it, `git mv doc/rules/Leaf_and_Let_Die_v37-03.pdf doc/legacy/Leaf_and_Let_Die_v37-03.pdf` (from repository root), or retain it in place until all reference links have been migrated. Do **not** delete old rulebooks/legacy experimental scripts simply because the default changed.
- The new PDF (p. 20) has a **printing typo**: `Bursting Blossom (74)`; the attached Plant CSV assigns it to the Flower 17 tier. Correct the rulebook master/source for the next version, rather than editing the provided PDF invisibly.

### Installation

From the top-level `leaf` repository directory, with the ZIP downloaded to `~/Downloads`:

```bash
unzip -o ~/Downloads/leaf-v38-rulebook-and-audit.zip
# optional, to archive old rules if they are tracked by Git:
mkdir -p doc/legacy
git mv doc/rules/Leaf_and_Let_Die_v37-03.pdf doc/legacy/Leaf_and_Let_Die_v37-03.pdf
# review only these documentation changes before commit:
git status --short
```

Suggested documentation-only commit: `docs(rules): add v38-01 rulebook and battle migration audit`.

**Important:** This ZIP does not install new official CSVs or modify Kotlin; do not assume simulation has adopted v38 just because the PDF was added.
