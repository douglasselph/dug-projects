# R0 — Correctness audit before question-driven card-effect research

**Audit basis:** user source archive `leaf(20261024-155145).tgz`, received October 24, 2026.  
**Scope:** no card, policy, game-rule, or experiment-runner behavior changed by R0.

Read **`doc/SIMULATION_LAUNCH_ARCHITECTURE.md` first** before any R1/R2 change to experiment scripts. The related `doc/USING_THE_SIMULATOR.md` and `doc/task12-experiment-family-migration.md` describe the direct-Java / compile-once / process-isolation contracts.

## R0 verdict and coverage

| Contract | Finding | Evidence / limitations |
|---|---|---|
| Aggregate Buy power | **Verified** | `BuyCoordinator.legalItems()` sums all Hand die values and all current Critter values; `validatePayment()` sums all selected dice and Critters. New R0 test purchases cost-17 Flower with dice 12+9, payment 21, overpay 4. |
| Buy payment / reuse | **Verified in focused tests** | Existing tests cover invalid payment, duplicate equivalent dice, overpayment, multiple purchases, Critter value boosts and no die reuse. |
| Grafting | **Verified in focused tests** | `Creature.legalPlacements()` supplies placements; `GraftResolver.prepare()` selects a legal placement; `resolve()` rechecks, then grafts face-down. New R0 test confirms an affordable Flower is *not offered* without a Vine. |
| Canonical Plants | **Verified** | `PlantCardRegistry` reads canonical Root/VF CSVs: 36 distinct cards. Internal IDs remove the source CSV's `z` sort prefix for Flowers. |
| Canonical Wisps / Rounds | **Verified for load/count** | Canonical CSVs parse 12 distinct Wisp definitions and 12 Round definitions. R0 is not a per-effect exhaustive semantic proof. |
| Active first-game Grove | **Verified** | `FirstGameDefault.PLANT_NAMES` has one card in each of R5/R7/R9/V7/V9/V11/F11/F14/F17. Exact names below. |
| Sapping Snapdragon current effect | **Verified** | Canonical CSV: cost-11 Flower, `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW`. Existing execution regression tests pass. |
| Sapping alternate effect | **Verified executable in unit tests** | Enhanced enum `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW`: Battle +2 to first selected die, select *different* own placed die, reduce opponents in its Strike Row, raise that anchor by amount reduced. Requires two own placed Battle dice. Cultivation remains Raise +2. This is **not** silently applied by the canonical CSV. |
| Strategy / mechanical RNG | **Verified for one seeded full-game path** | `GameFactory` supplies per-player strategy RNG distinct from mechanical RNG; `ShadowBuyStrategy` has isolated shadow seed. New seeded end-to-end test compares full `GameRunner` result AND entire chronicle with shadow observer/development tracking OFF versus ON; both match. This cannot prove every stochastic path. |
| Source provenance | **Inspectable** | New read-only `bin/r0_audit_inputs.py` produces combined main/simulation-source SHA256, canonical input SHA256s, current nine-card names, and optional Buy weight-file SHA256 and metadata. |
| Trained weight catalog compatibility | **Guard tested; current-market weights not supplied** | `LearnedBuyCardCatalog.prepare()` refuses trained weights when Plant definitions' fingerprint changes. New R0 test verifies that changing the Sapping effect makes the previous trained catalog invalid. The 15 `human-expert-resync/...` frozen learner weights are **not in this source archive**; validate each separately when R1 needs it. |
| Diagnostics noninterference (previous full D10 studies) | **Previously reported pilot evidence, not newly rerun** | The earlier 2/3/4-player D10 pilot archives passed ON/OFF comparisons in prior work. R0 adds an independent deterministic seeded full-game test in this snapshot but does not reproduce all prior pilot evaluations. |

### Concrete default Grove from current source

| Slot | Card title | Current ID |
|---|---|---|
| Root 5 | Root Four More | `Root_05_02` |
| Root 7 | Root Loot | `Root_07_04` |
| Root 9 | Root Kindred | `Root_09_03` |
| Vine 7 | Shift Happens | `Vine_07_01` |
| Vine 9 | Low & Behold | `Vine_09_01` |
| Vine 11 | Vine's the Limit | `Vine_11_04` |
| Flower 11 | Sapping Snapdragon | `Flower_11_03` |
| Flower 14 | Bloom Backflip | `Flower_14_02` |
| Flower 17 | Queen’s Blossom | `Flower_17_04` |

**R1 requirement:** every experiment must explicitly declare and report **all nine titles and slots**, rather than silently defaulting to `FirstGameDefault`. Changes to effective card cost/type must be resolved to the correct nine slots.

### Current source/data fingerprints (audit archive)

The SHA256s below identify the inputs **as uploaded**, not the prospective R1 or altered-effects runs:

| Input | SHA256 |
|---|---|
| Canonical Root CSV | `675f0d503234e76a8ec56f3bf2ad3faaf38a2484761dea668010fac10a86f028` |
| Canonical Vine/Flower CSV | `00d2d208100ddabc722b0d5605bcaa8ae05723667e60ae498b9d2f712c2ef9bd` |
| Canonical Round CSV (`Turn_Cards.csv`) | `e0be7625d20d2689fb4772b2d12919f28f88d68098eda39d8a4d2af992604e93` |
| Canonical Wisp CSV | `51bd4a4703997caad0f1ee4281696e906526e07ae85f0b4f42e967268fa72ccf` |
| Opt-in Plant resync CSV | `c64b7f237909435fb92580e20f7d8e5b5a6927dfa0755114ffcc859685b9fc2f` |
| Opt-in Round resync CSV | `0f7e49ddc5f96d4bd224a46060bec46eb05e450197b9917e9441157bf451060e` |
| Combined source digest (main+simulation Kotlin, Gradle settings/build) | `3189d97dd207f1e37b278eed79a4278cc71209eefbff547f2fd923a24fdc0c14` |

Compute fingerprints on the actual checkout before a new study:

```bash
bin/r0_audit_inputs.py --out ~/Downloads/leaf-r0-input-inventory.json
# When using a specific trained current-market expert:
bin/r0_audit_inputs.py --weights /absolute/path/to/learner-1.weights
```

The script deliberately **rejects** supplied trained weights whose stored card IDs/titles are inconsistent with the current CSV. In this archive, `data/ai/4p/buy-policy-v1.weights` fails with `Card title mismatch for Root_07_01`: **this older snapshot is not an approved official-current-market expert**. The optional script's checks are preliminary; the Kotlin evaluator performs the authoritative semantic card catalog fingerprint check, including cost and effect. No existing weights were modified or promoted.

### Validation performed in this audit environment

Command (with offline Gradle 8.13 cache made available):

```bash
./gradlew --offline --no-daemon test \
  --tests '*R0CorrectnessAuditTest' \
  --tests '*BuyCoordinatorTest' \
  --tests '*SappingSnapdragonVariantsRegressionTest' \
  --tests '*OfficialCardSourceTest' \
  --tests '*GameIsolationLifecycleTest' \
  --tests '*ShadowBuyStrategyTest' \
  --tests '*BuyDiagnosticNoninterferenceTest' \
  --tests '*DevelopmentNoninterferenceTest'
```

**Actual result: BUILD SUCCESSFUL, 41 tests, zero failures/errors**, including three R0-specific tests, two new BuyCoordinator regressions, Sapping variant execution tests and prior observer noninterference tests. `bin/r0_audit_inputs.py` also executed successfully without weights and passed Python compilation. Full `integrationTest` and `simulationTest` suites were **not** run in R0.

## Correctness concerns to carry into R1 (not changed by R0)

1. **Do not reuse old `data/ai` weights implicitly.** Existing reference weights may have stale titles/catalog fingerprints. Previous official 15 frozen learners were stored in output paths, but are absent here; validate exact files in the actual checkout when needed.
2. **The old D9 `bin/evaluate_buy_disagreements` currently runs `./gradlew ... runEvaluateBuyPolicy` once per chunk.** This predates the documented direct-Java launcher architecture. R0 intentionally does not edit it because replacing a launcher is not a correctness-audit change. Any future migration must compile once, execute each required isolated condition with direct Java, and preserve seeds, results, checkpoint semantics, logs, timing and archive contracts.
3. **The D9 experiment lock records Git revision and weight SHA but not explicit raw CSV and Grove fingerprints.** The new R0 inventory makes these visible. Future question-driven runner manifests must include source/input SHA, effective nine-card Grove, variant-effect fingerprint, exact weight/policy fingerprint and seed family; refuse resume on any mismatch.
4. **The current Buy-evaluation default is `FirstGameDefault`, not random official-market card selection.** R1 must explicitly select a Grove that includes the tested Sapping variant and report that resolved Grove.
5. **A card-effect result needs independent strategic evaluation.** R0 verifies game mechanics; it does not establish the strength/weakness of the Sapping effect or either variant. Do not use legacy Human-Baseline-beating preferences as proof of balance.

## R0 deliverable boundaries

- New focused regression tests and a read-only input audit utility.
- **No** source production logic changed; canonical CSVs, Sapping effect definitions, engine rules, Human Baseline, learned experts, RNG or historical scripts untouched.
- R1 is the question-driven Sapping experiment specification and first controlled evaluation, **not** an automatic general-purpose self-play AI rewrite.
