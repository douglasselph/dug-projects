# Capture Existing Human Baseline Build Behavior

**Purpose:** Preserve observable decisions from the *current* Human Baseline, before replacing Build valuations or adding Water, Sunlight and Mulch personalities. The snapshot is **diagnostic evidence**, not a balanced-game or optimal-strategy test.

## What has been implemented

The opt-in snapshot runner:

1. Loads both current official Plant CSVs plus current Round/Wisp catalogs. Requires **36 distinct canonical Plant IDs** and a matching registered Human Baseline scorer for each; separately reports whether the effective `GameEffect` also routes to that scorer by effect identity. Each row records its canonical ID, current title, *actual parsed* effect, scorer identity/effect, and existing Cultivation/Battle priority bases. Research variants and obsolete sorting-prefixed IDs are not used as substitute identities.
2. Runs complete v38 `THREE_ENGAGEMENTS` games with two Human Baseline players, standard Round setup, one explicit legal nine-card Grove, mechanical seed 163000 and strategy seed 263000 by default. Subsequent games increment each seed by one. The result is **observed original behavior**; no personality, tuned weights or learned player is introduced.
3. Records every engine-legal Cultivation Main-action kind, **every legally activatable Plant ID**, each Round slot's current `GameEffect`, the actually selected Main/Plant, and the Human Baseline's pre-separate-policy selection. A support-first choice is recorded as such instead of being mistaken for Draw/Plant/Round. Cultivation Support decisions separately retain the legal support list and selected/pass result.
4. Records the scored candidate alternatives and score adjustments from the existing reasoning sink. **Scored alternatives are not necessarily the entire legal set:** existing Compost/Mulch and other willingness gates can remove legal choices before final scoring. The real legal set must be read from `build-choices.tsv`, not inferred from `scored-main-candidates.tsv`.
5. Captures all-source physical Sunlight transfers, Mulch storage, Water and Mulch Support uses, basic `GAIN_WATER_TOKEN` resolutions, and final held resource counts. Separately records precise **Round-sourced** Water/Sunlight/Mulch acquisition-to-use lineages (including unused tokens and what they enabled). Neither source should be silently treated as a complete causal attribution of all effects: Water acquired by compound effects is not included in the basic Water gain counter; Round lineage excludes Plant/Wisp sources.
6. Checks that selected Main actions and chosen Plants were among the engine-legal offers at that decision. Unit/simulation tests verify current effect route coverage and repeatable same-seed evidence. This does **not** imply correctness of historical heuristic values.

## How to execute on Ubuntu

From within the repository root (`leaf/`):

```bash
unzip -o ~/Downloads/leaf-human-baseline-build-snapshot-20261116.zip
bin/capture_human_baseline_build
```

The executable script first runs the focused Kotlin regression test (`BuildBehaviorSnapshotTest`) and writes the simulation runtime classpath. It then starts one direct-Java process to play **four** fixed-seed games by default. Even if either stage fails it packages the log and status into `~/Downloads/leaf-human-baseline-build-<timestamp>.tgz`, printing `UPLOAD THIS FILE:`.

Optional small/large cohorts (subject to a maximum 100 games per invocation):

```bash
bin/capture_human_baseline_build --games=12 --players=2 --base-seed=163000 --strategy-seed=263000
```

Report files (when the Java experiment completes):

| File | What it means |
| --- | --- |
| `manifest.txt` | Seeds, player count, v38 rules, current Grove, SHA-256 catalog/source hashes, and Git-state files beside the report |
| `official-plant-scorer-inventory.tsv` | Current 36 canonical Plants, effective effects and scored route/priority bases |
| `build-choices.tsv` | **Engine-legal** Main kinds and Plants for each Build decision, actual choice, and Round effects |
| `support-choices.tsv` | Available and chosen Cultivation Supports (including Pass) |
| `scored-main-candidates.tsv` | Old scorer's considered candidates and their legacy scores/adjustments, *after* willingness gates |
| `resource-events.tsv` | Physical Water, Mulch and Sunlight acquisitions/uses observable from the Chronicle |
| `round-resource-lineages.tsv` | Round-specific Water, Sunlight and Mulch downstream use or unused status |
| `end-inventory.tsv` | Held Water, Sunlight, Mulch and pending Mulch per player at game end |
| `run.log`, `status.txt` | Test/run evidence; `PASS_OBSERVATION_ONLY` means capture succeeded, not that strategy quality is certified |

## Acceptance and subsequent comparison

- First **run the capture successfully on Ubuntu**, then upload the archive. Archive/provenance must identify precisely the current source and official catalogs (ideally commit the code immediately before running). The first actual results have not been produced in this environment.
- Check all 36 name-based mappings, **effect-based scorer-route gaps**, meaningful Build decision and scored-opportunity rows, and resource gain/spend accounting. The November 16 source has three known missing effect routes: `Root_07_02` (Root Appreciation), `Flower_11_01` (Alluring Nectar), and `Flower_14_01` (Bee-loved Bloom). These are observed source gaps, not missing official effect implementations; do not fix their fallback behavior as part of the unchanged-policy baseline. Some resource categories may have zero observations in a four-game sample; this is **not** a failed scorer mapping. Extend the small cohort only if needed for exposure.
- Repeat the same configuration with the same seeds and compare report bytes. A new Build valuation release should intentionally produce changed choices relative to this archive; it must not silently change legal offers, card identities or official mechanics.
- These are **behavioral records**, not pass/fail claims about which Main action is strategically best, nor proof that a card is weak or strong. Later personality tests must compare legally offered opportunities and useful spending rather than merely final inventory or win rate.

**November 16 first-run update:** The uploaded `leaf-human-baseline-build-20261116-210640.tgz` did **not** capture games. It stopped at Kotlin compilation: `BuildBehaviorSnapshotMain.kt` referenced `CardDataFiles.WISP_CARD_LIST` instead of `WISP_LIST`. The subsequent shared-immediate-pip change fixes that single constant; rerun the script before accepting baseline evidence. This failure must not be treated as four baseline games.

**No game rules, official CSV definitions, purchased dice, expert training, or Human Baseline priorities are changed by this observation work.** The small production change merely fills `RoundEffectChoice.legalPlantCardNames`, a previously empty reporting field, from the coordinator's existing legal choices.

## November 18 follow-up — immutable reference versus current capture

The November 16 `leaf-human-baseline-build-20261116-214822.tgz` output is the **saved pre-change reference**, not something to regenerate using today's updated code. The current launcher still records the same TSV schema, but now identifies the current Compost dice-lifetime scorer in its manifest. Use `bin/compare_human_baseline_build` to run current seeded games and report observed before/after choices, with strict provenance checks. See [`COMPOST_BUILD_DECISION_COMPARISON.md`](COMPOST_BUILD_DECISION_COMPARISON.md) for caveats about changed game trajectories and absence of same-state comparisons.
