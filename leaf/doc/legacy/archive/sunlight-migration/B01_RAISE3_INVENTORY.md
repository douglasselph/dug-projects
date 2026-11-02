# B01 — Legacy `RAISE_DIE_PLUS_3` Inventory

**Disposition:** Audited; no code or canonical data changed. This is an inventory/checkpoint for the later deliberate removal of obsolete Sunlight Raise +3.

**Source archive:** `leaf(20261026-012547).tgz` · SHA-256 `500556e827cce0778d716dcb4a5ee55222d13f1fbe67b45ee11c66c61698cdd5` · 1662 filesystem entries extracted. Git metadata is not part of this archive.

**Exact matches:** 88 textual occurrences across 38 files. **Indirect old-path matches recorded:** 57 additional line occurrences across 13 files. CSV: `doc/B01_RAISE3_REFERENCES.csv` has **145** line-level records with source excerpts. Indirect list is deliberately targeted; incidental uses of the generic number 3 are not all part of obsolete Sunlight.

## Source-of-truth determination

- Canonical `data/Turn_Cards.csv` contains precisely three Cultivation Sunlight entries (`Resource_Sunlight_Compost`, `Resource_Sunlight_Mulch`, `Resource_Sunlight_Water`), each reading **“Gain Sunlight token”**. No active Round CSV assigns `RAISE_DIE_PLUS_3`.
- `data/research/round-overrides/sunlight-token.csv` and archived `data/research/resync/round-resync-pre-official-20261023.csv` assign `GAIN_SUNLIGHT_TOKEN`; `data/research/resync/round-resync-current.csv` contains no Raise +3 assignment.
- The correct `GameEffectConverter` token alias is present near line 67; `ResourceEffectHandler` (lines 46, 122), `DefaultGameEffectExecutor` (line 186), and `SunlightTokenPriority` are the **current, separate** token paths. Keep them.
- `RAISE_DIE_PLUS_3` is **not a current Sunlight rule**. Nevertheless, the converter retains an older text alias and engine handlers still execute it when artificially invoked or when older experimental inputs explicitly select it. This is not merely a dead constant.

## Exact production and simulation call sites — removal dependency map

`src/main/kotlin/dugsolutions/leaf/v35/` prefixes are shortened to `v35/` below. Every line reference is to the uploaded archive; see the CSV for all exact textual occurrences.

| Area | File and line(s) | Current role | Follow-up consideration |
|---|---|---|---|
| Enum / feature identity | `effect/GameEffect.kt:69` | Defines obsolete constant | Examine trained-feature serialization before deleting enum entry. |
| Printed-text conversion | `effect/GameEffectConverter.kt:541–543` | Maps legacy “🎲 Raise a die +3” text to obsolete effect | Retire alias or reject as obsolete; leave “Gain Sunlight token” converter intact. |
| Executor routing | `effect/DefaultGameEffectExecutor.kt:154` | Still routes old effect to die-value handler | Remove after compatibility/feature impact review. |
| Mechanical legality | `effect/handler/DieValueEffectHandler.kt:39` | `canExecute` when Hand nonempty | Old effect remains mechanically executable. |
| Mechanical execution | `effect/handler/DieValueEffectHandler.kt:177–178` | Calls `raiseOne(request, 3)` | Delete only in dedicated, A03-approved mechanical cleanup. |
| Choice requirements | `effect/GameEffectDecisionRequirements.kt:231` | Classifies required die choice for old effect | Retire with engine path. |
| Cultivation old scorer | `player/decision/baseline/cultivation/resource/SunlightPriority.kt:10–81` | Dedicated old +3 value/target heuristic; no enum literal | Remove old scorer/callers; **not** `SunlightTokenPriority`. |
| Cultivation scored-mode gate | `player/decision/baseline/cultivation/HumanBaselineCultivationStrategy.kt:182–185` | Adds weak-Sunlight penalty for old effect | Obsolete for canonical token gain. |
| Cultivation identity bucket | Same `:238` | Groups old Raise +3 and token gain under “sunlight” | Untangle in scoring recalibration. |
| Cultivation old willingness | Same `:433–457` | Compares old +3 to Draw, applies seeded percentage gate and provenance | Obsolete branch; current token path should not inherit pip gate. |
| Cultivation old scorer dispatch | Same `:680` | Calls `SunlightPriority.score` for old effect | Remove obsolete case with old path. |
| Legacy policy knob | `player/decision/baseline/HumanBaselinePolicy.kt:51,151,531–532` | Stores `sunlightUsePercentage` for old willingness | Check public constructors/tests before deleting; not necessarily used by token scorer. |
| Generic Cultivation/Battle die targeting | `player/decision/baseline/effect/HumanBaselineEffectStrategy.kt:239,904,1257` | Old die target score, effect classification, and predicted pip gain | Remove obsolete cases but keep valid +1/+4/+20 transforms. |
| Generic target helper | `player/decision/baseline/card/CardScoringHelpers.kt:579` | Old +3 die target value | Remove obsolete branch only. |
| Battle-enabled Plant projection | `player/decision/baseline/battle/BattleEnabledPlantAnalyzer.kt:97` | Projects old Raise +3 through Strike calculations | Legacy conditional, not a canonical Sunlight card. |
| Simulation categorization | `src/simulation/kotlin/dugsolutions/leaf/simulation/v35/learning/buy/EvaluateBuyPolicyMain.kt:966` | Classifies obsolete effect under “Raise” | Keep historic report compatibility in mind before removal. |
| Scored Human configuration | `data/ai/human-scoring.properties` (`sunlight.variability`, `sunlight.weak`) | Former +3 knobs; current `sunlight` action bucket also covers token | Verify which knobs affect active token path before renaming/removing. |

**Mechanics risk:** An explicit `GameEffect.RAISE_DIE_PLUS_3` call currently changes dice. B01 should not be interpreted as disabling that route.

## Automated tests / fixtures that will need review in B02

**Unit test source** (13 files; 35 exact mentions):

- `src/test/kotlin/dugsolutions/leaf/v35/chronicle/ChronicleTextRendererTest.kt`: 641, 656, 665, 676
- `src/test/kotlin/dugsolutions/leaf/v35/chronicle/GameChronicleTest.kt`: 155, 166
- `src/test/kotlin/dugsolutions/leaf/v35/effect/DefaultGameEffectExecutorRoutingTest.kt`: 56, 208
- `src/test/kotlin/dugsolutions/leaf/v35/effect/DefaultGameEffectExecutorTest.kt`: 37, 44, 60, 66, 198, 221, 227
- `src/test/kotlin/dugsolutions/leaf/v35/effect/GameEffectConverterTest.kt`: 148
- `src/test/kotlin/dugsolutions/leaf/v35/effect/handler/DieValueEffectHandlerTest.kt`: 25, 48
- `src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/battle/BattleTurnOrchestratorTest.kt`: 100, 101, 151, 152
- `src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/battle/SunlightBattleSpendingValueTest.kt`: 144
- `src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/HumanBaselineCultivationStrategyTest.kt`: 346, 379, 426, 466, 503, 522
- `src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/effect/HumanBaselineEffectStrategyBattleOwnDieAlignmentTest.kt`: 85
- `src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/effect/HumanBaselineEffectStrategyTest.kt`: 36
- `src/test/kotlin/dugsolutions/leaf/v35/player/decision/effect/BaselineEffectStrategyTest.kt`: 23, 262
- `src/test/kotlin/dugsolutions/leaf/v35/round/A05SunlightSourceOfTruthTest.kt`: 60, 61

**Simulation tests** (2 files; 7 exact mentions):

- `src/simulationTest/kotlin/dugsolutions/leaf/simulation/v35/experiment/round/RoundExperimentConfigLoaderTest.kt`: 14, 15, 24, 37, 56
- `src/simulationTest/kotlin/dugsolutions/leaf/simulation/v35/learning/buy/EvaluateBuyPolicyTest.kt`: 448, 476

Tests fall into these groups:

1. **Explicit mechanics tests** (`DefaultGameEffectExecutorTest`, `DieValueEffectHandlerTest`, routing/choice tests) intentionally execute old Raise +3; these must change to tests that reject the retired effect or exercise a canonical alternative.
2. **Cultivation scoring tests** (`HumanBaselineCultivationStrategyTest`, `CultivationMainPriorityTest`) expect the old pip-based Sunlight gate; replace with tests of token valuation. Note `CultivationMainPriorityTest` calls `SunlightPriority` **without** mentioning the enum.
3. **Battle fixtures** (`SunlightBattleSpendingValueTest`, `BattleTurnOrchestratorTest`) use Raise +3 as an artificial weak Round effect; replace with another supported low-value effect without assuming Sunlight is a pip raise.
4. **Chronicle/rendering snapshots** record the old enum and old textual output; update fixture/snapshot expectations deliberately while preserving the ability to read historical logs if required.
5. **Research config fixtures** (`RoundExperimentConfigLoaderTest`, `EvaluateBuyPolicyTest`) construct obsolete Sunlight rounds; update to token-based definitions without obscuring what override experiments actually test.
6. **A05 source-of-truth checks** intentionally mention the forbidden legacy effect *as a negative assertion*; preserve those tests rather than blindly deleting every string occurrence.

## Seed/config/weights/historical experiments

- **Official seed cards:** no exact `RAISE_DIE_PLUS_3` occurrences in `data/Turn_Cards.csv`, `data/Cultivation_Cards - Root.csv`, `data/Cultivation_Cards - VF.csv`, or `data/Wisp_Cards.csv`. The 3 Sunlight round cards all grant tokens.
- **Research seed configs:** no exact occurrences in `data/research/` CSVs; the dated resync record references token gain, not a raise.
- **Persisted learned features:** 21 exact references across 6 `data/ai/**/*.weights` files. Most coefficients are zero; at least `data/ai/4p/cultivation-support-policy-v1.weights:671` has **nonzero 0.16269350899404136** on `ACTION_WORM_FLIP__EFFECT_RAISE_DIE_PLUS_3`. **Do not strip or rewrite these frozen weights as part of B02.** Their old feature keys can be retained as read-only compatibility artifacts.
- **Historical sanity launcher:** `bin/legacy/experiment_sunlight_token_sanity:7,70,103` calls Raise +3 “canonical” and compares it against token-gain override. This description is historical and **factually obsolete for current play**. The launcher is in `bin/legacy/`, but future users could still run it. Deprecate or relabel it without rewriting historic findings.
- **Historical documentation:** `doc/HUMAN_BASELINE_CULTIVATION_PLAN.md:59,303,428` describes Sunlight-as-raise, and `doc/HUMAN_BASELINE_CULTIVATION.md`, `doc/HUMAN_BASELINE_BATTLE.md`, `doc/HUMAN_BASELINE_BATTLE_PLAN.md`, and `doc/HUMAN_BASELINE_DECISION_INVENTORY.md` preserve old scoring/analysis. Mark as historical rather than silently rewriting their timeline.
- **R3B.1 uploaded result archive:** `d15de559a9fdd297f662.tgz` and its `r3b1-effect-aware.log` have **zero** exact or “Raise +3” text matches in the accessible archive/log. This does not mean their old policy weights lack any legacy feature definitions.
- **Scope caveat:** the latest source archive contains no `output/experiments/` directory and no earlier experiment `.tgz` outputs. The historical user-machine experiment results that have **not** been uploaded cannot be audited from this source snapshot. Before B02, optionally search local `output/experiments` archives/logs for `RAISE_DIE_PLUS_3`, `Raise +3`, `SunlightPriority` and historical manifest labeling.

## Suggested sequence for B02 (not performed by this audit)

1. Preserve B01 manifests and establish a normal Sunlight token integration test through the real CSV (A05 already supplies three cases).
2. Decide compatibility treatment for stored enum names/learned feature keys and historical chronicles. Do **not** mutate frozen `.weights`.
3. Deprecate/reject old printed-text converter alias and mechanical execution route in an **explicit A03-approved** change.
4. Remove old Cultivation willingness, `SunlightPriority`, die-targeting, generic target helpers, and Battle projector branches **only where they reference the retired effect**. Retain canonical token and Battle Sunlight-spending evaluators.
5. Update old tests/fixtures based on their intended behavior; retain negative tests confirming old Sunlight cannot raise dice.
6. Treat legacy experiment launcher as historic, not current canonical A/B research; label clearly or disable for normal launches.
7. Re-run compile, unit tests, simulation tests, A03 boundary checks, A05 canonical Sunlight integration, and the calibrated research preflight.

## Independent reproduction commands

```bash
rg -n --hidden 'RAISE_DIE_PLUS_3' src bin data doc
rg -n -i 'SunlightPriority|sunlightUsePercentage|sunlight\.weak|sunlight\.variability|Raise a die \+3|Sunlight Raise\+3' src bin data doc
rg -n -i 'RAISE_DIE_PLUS_3|Raise \+3|SunlightPriority' output/experiments  # if local outputs exist
```

**B01 is an inventory, not removal or proof of full source compatibility.** No code, mechanics, weights, seed data, or launchers are modified in this patch.

## Literal occurrences by code/document group

| Group | Occurrences | Files |
|---|---:|---:|
| Production Kotlin | 16 | 9 |
| Simulation Kotlin | 1 | 1 |
| Unit tests | 35 | 13 |
| Simulation tests | 7 | 2 |
| Persisted learned features | 21 | 6 |
| Documents | 8 | 7 |

`bin/legacy/experiment_sunlight_token_sanity` and `SunlightPriority.kt` are captured in the *indirect* portion of the companion CSV (no exact enum text required).
