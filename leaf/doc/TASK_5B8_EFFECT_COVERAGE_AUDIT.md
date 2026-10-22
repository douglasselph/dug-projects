# Task 5B.8 — Human Baseline Plant and Wisp effect coverage audit

Source: `leaf(20261022-214526).tgz`. Static source inspection only. **No changes to gameplay or scoring.**

## Reading this matrix

- **P — Specialized tactical projection:** A dedicated consequence evaluator is called for some phase or target decisions; **not** proof of full-game completeness.
- **M — Partial/state-informed:** Uses actual state, die caps, or resource availability, but may not project all decisions or downstream effects.
- **H — Heuristic/incomplete:** Fixed bonuses, rough substitutes, missing multistep projection, or target/activation disparities are visible.
- The Legal/target column records the relevant option *family*, not a proof that every legality edge case is tested.
- `effective_effect` includes entries from `data/research/resync/resync-current.csv` when present. Original card-scorer enum values can differ.
- The fixed/proxy notes are **audit findings**, not automatic recommendations to eliminate heuristics.

**Inventory:** 48 distinct CSV card identities (36 Plants plus 12 Wisp rows; one Wispquake event); 45 card-specific scorer objects (36 Plant and 9 Wisp scorer objects) with Pollinating Wisp shared across four colors.

## Per-card matrix

| Card / ID | Current effect | Legal choices / target family | Projection | RNG / resources / fixed bonus gap |
|---|---|---|---|---|
| Root Double Down (`Root_05_01`) | `DOUBLE_ONE_DIE` | die choice | Heuristic / incomplete | Raise gain is state-aware; Battle row consequences not guaranteed |
| Root Four More (`Root_05_02`) | `RAISE_DIE_PLUS_4` | die choice | Heuristic / incomplete | Cultivation thresholds considered; Battle target needs shared row projection |
| Root on a Roll (`Root_05_03`) | `REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS` | die choice | Heuristic / incomplete | Fixed 10+sides/2 proxy; exact conditional mean and rewards exclusion matter |
| Root Well (`Root_05_04`) | `GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE` | Water/own-or-opponent die | Partial / state-informed | Reserve bonus 20/10/-10; compare best legal Battle reroll |
| Root & Scoot (`Root_07_01`) | `RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE` | die/Strike square | Heuristic / incomplete | Flat +25 escape bonus for -5 row; forecast wound and forfeited Strike value |
| Root Appreciation (`Root_07_02`) | `GAIN_WORM_AND_BOOST_WORMS_PLUS_1_THIS_ROUND` | resource, no die target | Partial / state-informed | Current worm count proxy; opportunity to spend worms later not projected |
| Root Down Payment (`Root_07_03`) | `GAIN_VP_PER_ONE_SHOWING` | die if set-to-1 variant | Partial / state-informed | Uses count of ones; score of sacrificed die is partial |
| Root Recall (`Root_07_04`) | `DISCARD_UP_TO_2_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE` | subset of dice | Heuristic / incomplete | Mean reroll per die rounded early; replacement order and cap matter |
| Root Awakening (`Root_09_01`) | `UPGRADE_DIE_AND_USE_NOW` | die/available new size | Heuristic / incomplete | Upgrade opportunity and expected die roll approximate, value depends on remaining game |
| Root Cause (`Root_09_02`) | `FLIP_OWN_DIE_TO_OPPOSITE_FACE` | die | Partial / state-informed | Capped gain known; contextual Battle consequences need alignment |
| Root Kindred (`Root_09_03`) | `SET_DIE_TO_MATCH_ANOTHER` | die pair | Partial / state-informed | Pair max gain considers caps; Battle consequences not fully integrated |
| Root Loot (`Root_09_04`) | `MULCH_DIE_FROM_DISCARD` | discard die | Partial / state-informed | Largest die heuristic and fixed +10 reserve, no full future timing |
| Berry Important / Berry Tasty (`Vine_07_01`) | `RAISE_ALL_D8S_PLUS_1` | eligible D8s | Partial / state-informed | Current effect RAISE_ALL_D8S_PLUS_1 not canonical; inspect its eligible targets |
| Shift Happens (`Vine_07_02`) | `FLIP_OWN_PLANT_OR_FLIP_OPPONENT_ROOT_OR_VINE_IN_BATTLE` | Plant/opponent Plant | Heuristic / incomplete | Flat +15 target existence proxy; defensive/opponent tempo effects need projected value |
| Vine and Again (`Vine_07_03`) | `REUSE_SPENT_ROOT_OR_VINE_EFFECT` | spent Plant | Heuristic / incomplete | spent count ×6 ignores selected effect’s current outcome |
| Vine Yield (`Vine_07_04`) | `RAISE_ANY_DIE_PLUS_1` | die | Partial / state-informed | Current end-game PER_GRAFTED_FLOWER scoring distinct from activation value |
| Low & Behold (`Vine_09_01`) | `SET_LOWEST_VALUE_DIE_TO_MAX` | lowest die | Partial / state-informed | Calculates best gain; strike placement context incomplete |
| Parting Thorn (`Vine_09_02`) | `FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE` | Plant/opponent/card | Heuristic / incomplete | Uses fixed +22 or count×5 instead of actual target utility |
| Rise and Vine (`Vine_09_03`) | `RAISE_ALL_DICE_PLUS_2` | all dice | Partial / state-informed | Real caps used; total Battle row effect not always projected |
| Vine and Dine (`Vine_09_04`) | `TRASH_CRITTER_TO_RAISE_DIE_PLUS_5` | critter/die | Heuristic / incomplete | Fixed -20 reserve penalty, no marginal supply valuation |
| Reap What You Roll (`Vine_11_01`) | `DISCARD_ONE_DIE_DRAW_TWO` | die and future draw placement | Heuristic / incomplete | Mean draw estimate; Battle placement and die loss must be evaluated jointly |
| Saplink Trellis (`Vine_11_02`) | `RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE` | sequence of dice | Partial / state-informed | Accounts for realizable capped raises but not best Battle row split |
| Vine and Punishment (`Vine_11_03`) | `REROLL_DIE_ON_3_DRAW_ONE_AND_REDUCE_OPPOSING_STRIKE_ROW_BY_3` | own die/row | Heuristic / incomplete | Current override differs from printed canonical; fixed +8 chance draw, proxy row score |
| Vine's the Limit (`Vine_11_04`) | `SET_DIE_UP_TO_D12_TO_MAX` | eligible die | Partial / state-informed | Best capped die gain; Battle row delta may dominate |
| Alluring Nectar (`Flower_11_01`) | `GAIN_OR_STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES` | Butterfly/opponent | Heuristic / incomplete | spent×10 and fixed ±15 acquisition proxy; distinguish identity/ownership |
| Bloom Backbone (`Flower_11_02`) | `RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER` | sequence of dice | Partial / state-informed | Capped raises counted, not full Battle placement-aware allocation |
| Sapping Snapdragon (`Flower_11_03`) | `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW` | one die or ordered pair | Partial / state-informed | Shared Sapping Battle projector exists; test activation/target consistency |
| Transplant Tulip (`Flower_11_04`) | `DRAW_ONE_DIE_AND_SWAP_TWO_OWN_DICE_RAISE_ONE_PLUS_2_IN_BATTLE` | draw placement/die pair | Heuristic / incomplete | Mean draw + fixed +16 swap bonus; no full combined projection |
| Bee-loved Bloom (`Flower_14_01`) | `GAIN_OR_STEAL_BEE_AND_BOOST_BEES_PLUS_2_THIS_ROUND` | Bee source/target | Heuristic / incomplete | Existing bees×12/10, not full choice plus resource/opponent denial |
| Bloom Backflip (`Flower_14_02`) | `RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW` | die/row | Heuristic / incomplete | Row-need proxy; flipping opponents should project full row outcome |
| Gust of Petals (`Flower_14_03`) | `REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW` | die/row | Heuristic / incomplete | Reroll mean + row-need/3; opponent reroll distribution approximated |
| Petal To Die 4 (`Flower_14_04`) | `GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW` | Gain/Steal source and D4 cycle | Partial / state-informed | Petal projector implemented; source-conditioned cycling approximation remains |
| Bursting Blossom (`Flower_17_01`) | `RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE` | die and ensuing draws | Heuristic / incomplete | maxNow×15 plus canCreate +18; misses full capped draw/capacity interaction |
| Forget-Me-Not (`Flower_17_02`) | `ROLL_DIE_FROM_DISCARD_INTO_HAND` | discard die/row placement | Partial / state-informed | Timing-sensitive FutureDiceAvailability exists; final Battle placement partial |
| O Edelweiss (`Flower_17_03`) | `PLAY_OR_FLIP_ANOTHER_CARD_TWICE` | two sequential Plant choices | Heuristic / incomplete | Battle evaluator explicitly one-step only; Cultivation count of face-down is outdated |
| Queen’s Blossom (`Flower_17_04`) | `DRAW_TWO_DICE` | draw destinations | Heuristic / incomplete | expectedNextDraw×5 rather than full Battle row and purchase threshold valuation |
| Wisp of Honor (`Wisp_Award_VP`) | `GAIN_ONE_VP` | play/hold | Partial / state-informed | Deterministic immediate VP; opportunity cost of held 2 VP and timing |
| Berry Patient (`Wisp_Award_VP2`) | `GAIN_ONE_VP` | play/hold | Partial / state-informed | Deterministic immediate VP; opportunity cost of held 3 VP and timing |
| Whispering Wings (`Wisp_Gain_Critters`) | `GAIN_ANY_TWO_CRITTERS` | Bee/Worm combinations | Heuristic / incomplete | Fixed base and target chooser; need explicit marginal Bee/Worm valuation |
| Pollinating Wisp (`Wisp_Gain_Green`) | `GAIN_OR_REFRESH_GREEN_BUTTERFLY` | Butterfly owner/color | Heuristic / incomplete | Pollinating group scorer; fixed reserve valuation may miss tactical timing |
| Pollinating Wisp (Purple) (`Wisp_Gain_Purple`) | `GAIN_OR_REFRESH_PURPLE_BUTTERFLY` | Butterfly owner/color | Heuristic / incomplete | Pollinating group scorer; fixed reserve valuation may miss tactical timing |
| Pollinating Wisp (Red) (`Wisp_Gain_Red`) | `GAIN_OR_REFRESH_RED_BUTTERFLY` | Butterfly owner/color | Heuristic / incomplete | Pollinating group scorer; fixed reserve valuation may miss tactical timing |
| Pollinating Wisp (Yellow) (`Wisp_Gain_Yellow`) | `GAIN_OR_REFRESH_YELLOW_BUTTERFLY` | Butterfly owner/color | Heuristic / incomplete | Pollinating group scorer; fixed reserve valuation may miss tactical timing |
| Pocketed Spark (`Wisp_Mulch_Die`) | `GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD` | discard die/Mulch capacity | Partial / state-informed | Die size and reserve approximation rather than projected use timing |
| Wispquake (`Wisp_Quake`) | `REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN` | die kept; opponents reroll | Heuristic / incomplete | Special deck-trigger event, not ordinary play decision; mean predictions may miss swing |
| Wisp_Reckoning (`Wisp_Reckoning`) | `LIMIT_WISPS_AND_TRASH_EXCESS` | effect-specific | Not classified | No verified classification |
| Pollen Theft (`Wisp_Swap_Die`) | `SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE` | cross-player die pair | Specialized tactical projection | BattlePollenTheftEvaluator models tactical effect and spending gate |
| Overgrowth (`Wisp_Upgrade_Die`) | `UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW` | die | Specialized tactical projection | BattleTwoStepUpgradeEvaluator and policy target alignment; Cultivation approximate |

## Conclusions and priorities

### Actual representation gaps (address before using Human as a strong control)

1. **O Edelweiss** (`Flower_17_03`): `BattleOEdelweissAnalyzer` explicitly evaluates only the current first step. `CardScoringHelpers` Cultivation branch still counts face-down cards as reusable despite face-up-only Play. Require true two-choice projection, legality recomputation, and consistency with execution.
2. **Complex Battle collateral effects**: Bloom Backflip, Gust of Petals, Transplant Tulip, and Vine and Punishment use partial row proxies or fixed swap bonuses in the generic scorer. Compare them with specialized target analyzers, then require activation/target consistency.
3. **Multi-draw and multi-resource effects**: Queen’s Blossom, Bursting Blossom, and Reap What You Roll use arithmetic-mean expected draws but do not always project availability, row capacity, and threshold effects jointly. A mean remains the specified prediction policy; do not sample mechanical RNG.
4. **Resource acquisition and opponent denial**: Bee-loved Bloom, Alluring Nectar, Parting Thorn, Shift Happens, Water/Worm/Mulch gain effects use fixed availability/reserve terms that should be checked against actual choice consequences.
5. **Nested reuse and multi-target selection**: Vine and Again uses spent-card count as a proxy instead of valuing the selected spent Root/Vine. Saplink Trellis and Bloom Backbone use realizable aggregate Raise value but can misvalue row allocation.
6. **Research override identity**: The active research file changes several effects (Berry, Root Appreciation/Recall/Down Payment, Vine and Punishment, Petal). Audit should match each effective override and compare with the canonical scorer code, not infer behavior from card title.

### Existing stronger implementations worth reusing

- `BattleSappingSnapdragonProjector` and `BattleSappingActivationPriority`: shared S1/S2 Battle evaluation with target/activation alignment (verify tests).
- `PetalAcquisitionProjection`: D4 source/destination acquisition and optional cycling (source-conditioned cycling still partial).
- `BattlePollenTheftEvaluator`, `BattleTwoStepUpgradeEvaluator`, `BattleImmediateStrikeResolveEvaluator`: effect-specific tactical valuation, subject to phase and opportunity boundaries.
- `BattleRowAssessor`, `BattleSwingEvaluator`, `BattleDiePlacementAnalyzer`: reusable state-sensitive Battle valuation.
- `PurchaseThresholdHeuristics` and `FutureDiceAvailability`: existing economic/timing approximations rather than full purchase-sequence optimization.

### Acceptable approximations under the agreed scoring contract

- For any **unknown die roll**, use `(sides + 1) / 2` exactly (D6 = 3.5), with no RNG, no Roll Rewards, and no fictional physical die face.
- Resource future value can use bounded, explicit proxies where the actual future decision sequence is unknown. Label the proxy and do not layer it over a projection already accounting for that consequence.
- Flat `cultivationPlayBase` / `battlePlayBase` are policy priors, not necessarily bugs; compare incremental contributions and avoid double-counting after a projected outcome.
- Some Wisp identities involve **hold versus play**; terminal hold VP belongs to the alternative, not a generic play bonus.

### Validation and confidence

- This is a **static audit of source implementation and the current research CSV**, not a passing-test certification or empirical learner result.
- Per-card labels are risk classifications, not a proof that the downstream chooser is correct for every legal state. No game simulations were run.
- Recommended next step: 5B.9, targeting the clearest H gaps; also finish 5B.5 (O Edelweiss two-choice planning), which remains outstanding.

### Principal files inspected

`HumanBaselineCardScorerRegistry.kt`, `HumanBaselineCardScorer.kt`, `CardScoringHelpers.kt`, `HumanBaselineEffectStrategy.kt`, `BattleOEdelweissAnalyzer.kt`, `BattleSappingSnapdragonProjector.kt`, `BattleSappingActivationPriority.kt`, `PetalAcquisitionProjection.kt`, `BattlePollenTheftEvaluator.kt`, `BattleTwoStepUpgradeEvaluator.kt`, `BattleImmediateStrikeResolveEvaluator.kt`, and `data/research/resync/resync-current.csv`.
