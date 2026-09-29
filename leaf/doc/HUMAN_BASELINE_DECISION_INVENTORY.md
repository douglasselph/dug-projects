# Human Baseline Decision Inventory

**Snapshot:** source archive supplied 2026-09-29 (`leaf(20260929-144826).tar`).

**Scope:** inventory/specification only. No gameplay or strategy behavior was changed. This document records what the current code does so later calibration work has a fixed starting point.

## Executive findings

- The Human Baseline is split across nine decision seams if `StrategyRandomizer` is included: Cultivation, Battle, Buy, Effect targeting/branching, Creature placement, Support, Reward, Wound, and strategy randomness.
- Cultivation action comparison already uses explainable `PriorityScore(base + adjustments)`. Selected-choice reasoning can already be emitted as typed `DecisionReasoning` and snapshotted into Chronicle. This means the planned observability work is **partially present already**; the current trace records the selected choice, not a complete retained table of every rejected alternative.
- Draw is not a fixed 50. Current Cultivation/Battle intrinsic Draw scoring is `35 + 4 × expectedRoll(nextSides)`; no drawable die scores 20. Therefore D4/D6/D8/D10/D12/D20 expected Draw scores are approximately 45/49/53/57/61/77 after integer rounding.
- Plant/Wisp base scores are explicitly documented in code as **strategy priorities, not measurements of objective card strength**. The code does not define a common empirical unit for “30”, “58”, “72”, etc.
- Several score constants have clear local meanings (for example +10 per crossed Buy tier), but their numeric scale is still calibration policy rather than an empirically established VP unit. The machine-readable appendix marks such constants as calibration values unless they are direct rule/mechanical constants.
- Cultivation Round effects have explicit scorers for Compost, Mulch, Water, and Sunlight. Any other Cultivation Round effect falls back to priority 45. Battle Round effects use a separate fixed mapping with fallback 45.
- Current Battle decision code is much richer than a flat card score: expected Draw placement, row transitions, wounds, support reachability/capacity, direct/enabling support analysis, continuation, and card-specific Battle analyzers are present.
- Existing source comments still contain legacy planning/checkpoint labels. This inventory intentionally does not edit them, but they violate the current project rule against planning IDs in production artifacts and should be removed in a later cleanup-only change.

## 1. Strategy decision seams

### RewardStrategy
- Methods: chooseCritter
- Source: `decision/reward/RewardStrategy.kt`

### WoundStrategy
- Methods: choose
- Source: `decision/wound/WoundStrategy.kt`

### BattleStrategy
- Methods: chooseFirstMainAction; chooseTurnAction; chooseDiePlacement
- Source: `decision/battle/BattleStrategy.kt`

### BuyStrategy
- Methods: choosePurchase; choosePayment
- Source: `decision/buy/BuyStrategy.kt`

### SupportStrategy
- Methods: chooseButterflyRoll
- Source: `decision/support/SupportAction.kt`

### EffectStrategy
- Methods: chooseDie; chooseBattleDie; chooseRootWellBattle; chooseCrossPlayerDieSwap; chooseOptionalDie; chooseDice; chooseDiePair; chooseOptionalDiePair; chooseCritterAndDie; choosePetalToDie4; chooseBeeSource; chooseButterflyTarget; chooseOptionalPlant; chooseOpponentPlantWound; choosePlantEffect; chooseOEdelweiss; chooseWispsToKeep; chooseDieSize; choosePlayer; chooseStrikeRow
- Source: `decision/effect/EffectStrategy.kt`

### CreaturePlacementStrategy
- Methods: choose
- Source: `decision/placement/CreaturePlacementStrategy.kt`

### CultivationStrategy
- Methods: chooseAction
- Source: `decision/cultivation/CultivationStrategy.kt`

### StrategyRandomizer
- Method: `nextInt(bound)` through the strategy-randomness seam.
- Purpose: keeps strategy variation separate from mechanical RNG.
- Source: `decision/random/StrategyRandomizer.kt`

## 2. Human Baseline implementations

- **HumanBaselineBattleStrategy** — chooseFirstMainAction; chooseTurnAction; chooseDiePlacement; `decision/baseline/battle/HumanBaselineBattleStrategy.kt`
- **HumanBaselineBuyPlanner** — supporting/director component; `decision/baseline/buy/HumanBaselineBuyPlanner.kt`
- **HumanBaselineBuyStrategy** — choosePurchase; choosePayment; `decision/baseline/buy/HumanBaselineBuyStrategy.kt`
- **HumanBaselineCardScorerRegistry** — supporting/director component; `decision/baseline/card/HumanBaselineCardScorerRegistry.kt`
- **HumanBaselineCreaturePlacementStrategy** — choose; `decision/baseline/placement/HumanBaselineCreaturePlacementStrategy.kt`
- **HumanBaselineCultivationStrategy** — chooseAction; `decision/baseline/cultivation/HumanBaselineCultivationStrategy.kt`
- **HumanBaselineDecisionDirector** — supporting/director component; `decision/baseline/HumanBaselineDecisionDirector.kt`
- **HumanBaselineEffectStrategy** — chooseDie; chooseBattleDie; chooseRootWellBattle; chooseCrossPlayerDieSwap; chooseOptionalDie; chooseDice; chooseDiePair; chooseOptionalDiePair; chooseCritterAndDie; choosePetalToDie4; chooseBeeSource; chooseButterflyTarget; chooseOptionalPlant; chooseOpponentPlantWound; choosePlantEffect; chooseOEdelweiss; chooseWispsToKeep; chooseDieSize; choosePlayer; chooseStrikeRow; `decision/baseline/effect/HumanBaselineEffectStrategy.kt`
- **HumanBaselinePolicy** — supporting/director component; `decision/baseline/HumanBaselinePolicy.kt`
- **HumanBaselineRewardStrategy** — chooseCritter; `decision/baseline/reward/HumanBaselineRewardStrategy.kt`
- **HumanBaselineSupportStrategy** — chooseButterflyRoll; `decision/baseline/support/HumanBaselineSupportStrategy.kt`
- **HumanBaselineWoundStrategy** — choose; `decision/baseline/wound/HumanBaselineWoundStrategy.kt`

The top-level `HumanBaselineDecisionDirector` wires the specialized strategies together. `HumanBaselinePolicy` centralizes many percentages, thresholds, development targets, and Battle policy parameters used by those strategies.

## 3. Cultivation action scoring

### Draw

Current formula:

    no drawable die: 20
    otherwise: 35 + round(4 × expectedRoll(next die sides))

The “next die” is inferred from the lowest-sided die in Supply, otherwise the lowest-sided die in Discard. The score measures immediate availability this round; the code explicitly says it does not add a long-term dice-development deficit bonus.

**Calibration status:** interpretable formula, but the 35 base and 4-points-per-expected-value scale have no documented empirical unit. They are calibration constants.

### Plant activation

Plant activation begins with the registered card scorer’s Cultivation base plus card/effect-specific visible-state adjustments from shared helpers. A guaranteed permanent dice-pool improvement (`UPGRADE_DIE_AND_USE_NOW`) can receive the policy’s modest dice-development bonus. There is no generic Plant-development bonus merely for activating an already-grafted Plant.

### Round effects

| Effect | Current top-level scoring | Important contextual behavior | Calibration note |
|---|---|---|---|
| Compost / `UPGRADE_DIE_FROM_HAND` | base 75; no target 15 | permanent side upgrade ×2; Buy-tier impact ×10; +2 per future Cultivation round up to 5; development bonus; -40 below 5 Hand-die Buy power | Explicit intent, but numeric scale is hand-authored calibration |
| Mulch / `MULCH_DIE_FROM_HAND` | base 45; no target 10 | only voluntarily values showing 1–4; +20 for 1–2; small high-side bonus capped 5; Buy-tier impact ×10; +10 while fewer than 2 prepared Mulches | Does not yet model D4 dilution/removal or recycle distance |
| Water / `GAIN_WATER_TOKEN` | base 30 | +20 for first Water; +10 for second; -15 with reserve ≥2 | Hand-authored reserve heuristic |
| Sunlight / `RAISE_DIE_PLUS_3` | base 35 | only considered when best actual +3 gain beats expected Draw; actual gain ×5; Buy-tier impact ×10 | Already explicitly compared with Draw in one narrow way |
| Any other Cultivation Round effect | 45 | no effect-specific Cultivation scoring here | Pure fallback constant; strongest “seemed about right” candidate |

Compost, Mulch, Sunlight, Overgrowth, and Pocketed Spark also have probabilistic willingness gates before score comparison. Those gates are part of Human Baseline behavior and must be calibrated separately from the score itself.

### Cultivation Supports

`CultivationSupportPriority` separately scores Water reroll, Water refresh, Mulch use, Worm flip, Butterfly use, and Wisp play. Its constants include bases around 20–30 plus effect-specific gain multipliers/divisors. These are strategy calibration values, not rule values.

## 4. Plant and Wisp base-score inventory

These bases are the current prior preference before contextual adjustments. They are **not objective strength ratings** and currently have no empirical unit.

| Card ID | Title | Effect | Cult base | Battle base |
|---|---|---|---:|---:|
| `Flower_11_01` | Alluring Nectar | `GAIN_OR_STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES` | 60 | 60 |
| `Flower_11_02` | Bloom Backbone | `RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER` | 70 | 70 |
| `Flower_11_03` | Sapping Snapdragon | `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW` | 55 | 82 |
| `Flower_11_04` | Transplant Tulip | `DRAW_ONE_DIE_AND_SWAP_TWO_OWN_DICE_RAISE_ONE_PLUS_2_IN_BATTLE` | 50 | 65 |
| `Flower_14_01` | Bee-loved Bloom | `GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND` | 68 | 68 |
| `Flower_14_02` | Bloom Backflip | `RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW` | 35 | 78 |
| `Flower_14_03` | Gust of Petals | `REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW` | 48 | 72 |
| `Flower_14_04` | Petal To Die 4 | `GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4` | 65 | 65 |
| `Flower_17_01` | Bursting Blossom | `RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE` | 82 | 82 |
| `Flower_17_02` | Forget-Me-Not | `ROLL_DIE_FROM_DISCARD_INTO_HAND` | 72 | 72 |
| `Flower_17_03` | O Edelweiss | `PLAY_OR_FLIP_ANOTHER_CARD_TWICE` | 85 | 85 |
| `Flower_17_04` | Queen’s Blossom | `DRAW_TWO_DICE` | 82 | 82 |
| `Root_05_01` | Root Double Down | `DOUBLE_ONE_DIE` | 55 | 55 |
| `Root_05_02` | Root Four More | `RAISE_DIE_PLUS_4` | 58 | 58 |
| `Root_05_03` | Root on a Roll | `REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS` | 45 | 45 |
| `Root_05_04` | Root Well | `GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE` | 40 | 65 |
| `Root_07_01` | Root & Scoot | `RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE` | 40 | 55 |
| `Root_07_02` | Root Appreciation | `GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND` | 60 | 60 |
| `Root_07_03` | Root Down Payment | `SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE` | 45 | 45 |
| `Root_07_04` | Root Recall | `DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE` | 55 | 55 |
| `Root_09_01` | Root Awakening | `UPGRADE_DIE_AND_USE_NOW` | 75 | 75 |
| `Root_09_02` | Root Cause | `FLIP_OWN_DIE_TO_OPPOSITE_FACE` | 55 | 55 |
| `Root_09_03` | Root Kindred | `SET_DIE_TO_MATCH_ANOTHER` | 55 | 55 |
| `Root_09_04` | Root Loot | `MULCH_DIE_FROM_DISCARD` | 65 | 65 |
| `Vine_07_01` | Berry Important | `RAISE_ANY_DIE_PLUS_1` | 30 | 30 |
| `Vine_07_02` | Shift Happens | `FLIP_OWN_PLANT_OR_FLIP_OPPONENT_ROOT_OR_VINE_IN_BATTLE` | 55 | 55 |
| `Vine_07_03` | Vine and Again | `REUSE_SPENT_ROOT_OR_VINE_EFFECT` | 60 | 60 |
| `Vine_07_04` | Vine Yield | `RAISE_ANY_DIE_PLUS_1` | 30 | 30 |
| `Vine_09_01` | Low & Behold | `SET_LOWEST_VALUE_DIE_TO_MAX` | 68 | 68 |
| `Vine_09_02` | Parting Thorn | `FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE` | 45 | 80 |
| `Vine_09_03` | Rise and Vine | `RAISE_ALL_DICE_PLUS_2` | 72 | 72 |
| `Vine_09_04` | Vine and Dine | `TRASH_CRITTER_TO_RAISE_DIE_PLUS_5` | 45 | 45 |
| `Vine_11_01` | Reap What You Roll | `DISCARD_ONE_DIE_DRAW_TWO` | 70 | 70 |
| `Vine_11_02` | Saplink Trellis | `RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE` | 65 | 65 |
| `Vine_11_03` | Vine and Punishment | `SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3` | 35 | 75 |
| `Vine_11_04` | Vine's the Limit | `SET_DIE_UP_TO_D12_TO_MAX` | 70 | 70 |
| `Wisp_Award_VP` | Wisp of Honor | `GAIN_ONE_VP` | 45 | 45 |
| `Wisp_Award_VP2` | Berry Patient | `GAIN_ONE_VP` | 45 | 45 |
| `Wisp_Gain_Critters` | Whispering Wings | `GAIN_ANY_TWO_CRITTERS` | 60 | 60 |
| `Wisp_Gain_Green` | Pollinating Wisp | `GAIN_OR_REFRESH_GREEN_BUTTERFLY` | 58 | 58 |
| `Wisp_Gain_Purple` | Pollinating Wisp | `GAIN_OR_REFRESH_GREEN_BUTTERFLY` | 58 | 58 |
| `Wisp_Gain_Red` | Pollinating Wisp | `GAIN_OR_REFRESH_GREEN_BUTTERFLY` | 58 | 58 |
| `Wisp_Gain_Yellow` | Pollinating Wisp | `GAIN_OR_REFRESH_GREEN_BUTTERFLY` | 58 | 58 |
| `Wisp_Mulch_Die` | Pocketed Spark | `GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD` | 72 | 72 |
| `Wisp_Quake` | Wispquake | `REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN` | 50 | 50 |
| `Wisp_Reckoning` |  | `LIMIT_WISPS_AND_TRASH_EXCESS` | 55 | 55 |
| `Wisp_Swap_Die` | Pollen Theft | `SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE` | 20 | 68 |
| `Wisp_Upgrade_Die` | Overgrowth | `UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW` | 85 | 85 |
| `Wisps_Resolve` | Wisp's Resolve | `RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW` | 20 | 75 |

Notes:

- `Vine_07_01` and `Vine_07_04` both currently have Cultivation/Battle base 30.
- `Flower_17_02` Forget-Me-Not currently has base 72 in both phases.
- `Flower_17_04` Queen’s Blossom currently has base 82 in both phases.
- Pollinating Wisp uses one scorer for four color variants; the registry maps the parallel color effects to that scorer.
- `Wisps_Resolve` is registered by `WispsLastWordBaseline` with Cultivation 20 / Battle 75.

## 5. Contextual card-score adjustments

Card bases are not the whole decision. `CardScoringHelpers` and specialized scorers add visible-state adjustments. Current families include immediate die-value gain, Buy-threshold crossings, resource reserve/value, permanent upgrades, card-specific branch value, Battle-specific target analysis, and preservation/influence adjustments.

Important limitation for the next review: many of these adjustments value the **current visible consequence**. Phase-boundary preservation, Creature-refresh sequencing, dice recycle distance, and delayed asset provenance are not uniformly represented.

## 6. Battle scoring inventory

Battle has three distinct layers that should not be confused with Plant base values:

1. **Intrinsic action priority** — Draw uses the same Draw priority; Plants use Battle card score; Battle Round effects use a fixed mapping.
2. **Tactical analysis** — expected/actual die placement and card/support analyzers measure row transitions, margins, wounds, VP impact, reachability, capacity, and enabling value.
3. **Turn orchestration** — compares first Main, Supports, enabling Supports, and continuation/final Main under Battle policy thresholds.

Current Battle Round-effect intrinsic mapping:

| Effect | Priority |
|---|---:|
| Gain 1 VP | 60 |
| Gain D20 to Discard | 65 |
| Gain D12 to Discard | 58 |
| Gain D10 to Discard | 54 |
| Gain 1 Wisp | 50 |
| Gain any die to Discard | 60 |
| Refresh Creature | 55 |
| Gain 2 Worms | 55 |
| Steal random Wisp from one opponent | 58 |
| Steal random Wisp from all opponents | 70 |
| Other/unmapped | 45 |

These values are fixed strategy priorities. No code-level empirical derivation is documented.

## 7. Effect target/branch hooks

`EffectStrategy` exposes focused legal-choice seams rather than hiding effect choices inside executors. Current hooks are:

- chooseDie; chooseBattleDie; chooseRootWellBattle; chooseCrossPlayerDieSwap; chooseOptionalDie; chooseDice; chooseDiePair; chooseOptionalDiePair; chooseCritterAndDie; choosePetalToDie4; chooseBeeSource; chooseButterflyTarget; chooseOptionalPlant; chooseOpponentPlantWound; choosePlantEffect; chooseOEdelweiss; chooseWispsToKeep; chooseDieSize; choosePlayer; chooseStrikeRow.

`HumanBaselineEffectStrategy` overrides these hooks with effect-aware selection. This is the downstream target/branch layer; it is distinct from the top-level decision “should I activate this card at all?”

## 8. Numerical-priority audit

The accompanying CSV contains every score/policy-like `const val` found under the Human Baseline package, with source line, current value, nearby documented meaning where available, and a calibration classification.

- **111** score/policy-like constants were inventoried.
- Direct rule/mechanical constants (for example wound margin 5 and base Strike VP 2) are marked separately from strategy calibration values.
- Most bases, bonuses, penalties, multipliers, percentages, thresholds, scales, and weights are marked **“calibration value; no empirical unit documented.”** This does not mean they are wrong; it means the current source does not establish that a point has a stable VP-equivalent or statistically calibrated meaning.
- The largest calibration risk is not any single number. It is comparing numbers from different heuristics as though they share a rigorously calibrated scale when that scale has not yet been demonstrated.

## 9. Existing test coverage

- The repository contains 199 Kotlin test files across unit, integration, and simulation-test source sets.
- 77 Human Baseline source components are directly referenced by at least one test file in the automated coverage scan.
- The CSV records the direct source-name-to-test-file references. This is a structural inventory, not code-coverage instrumentation: indirect behavior exercised without naming the implementation will not appear in this map.
- Existing documentation also includes focused Human Baseline documents for Cultivation, Battle, Effect Choices, Wound Resolution, Critter Reward, Butterfly Result, Graft Placement, testing, Chronicle/output, and simulation planning.

## 10. Chronicle and research evidence already available

The current Chronicle is typed. Relevant existing evidence includes:

- RoundRevealed and RoundCompleted with player snapshots;
- DieRolled and RollReward;
- MainAction and SupportAction, including some decision-probability metadata;
- EffectResolved with player, GameEffect, source kind/name, and phase;
- DecisionReasoning with selected choice label, base score, adjustments, and total;
- Purchase and Graft;
- BattleOrder, BattleGridReport, and BattleResolvePreview;
- FinalScore;
- compact simulation summaries and existing baseline/card/learned-Buy experiment aggregators.

What is **not yet represented as first-class research evidence** for the planned review:

- a retained score table for every legal alternative at each qualifying decision (selected-choice reasoning exists);
- Strike contribution provenance by source;
- individual decisiveness / wound-decisiveness attribution;
- delayed asset provenance such as Forget-Me-Not -> die -> Mulch -> later Battle use;
- targeted opportunity/outcome observations keyed to a card/effect;
- matched decision-level counterfactual outcomes.

## 11. Constants that most clearly need later calibration

This checkpoint does not tune them. The following are high-priority because they directly compare unlike actions on one score scale:

- Draw: base 35 and 4 points per expected die value;
- generic Cultivation Round fallback: 45;
- Compost base 75, +2 per upgrade side, +10 per Buy tier, +2 per future Cultivation round, -40 Hand-power-floor penalty;
- Mulch base 45, +20 low-roll bonus, +10 reserve bonus, high-side bonus formula, +10 per Buy tier;
- Water base/stock bonuses: 30/+20/+10/-15;
- Sunlight base 35, +5 per actual gain, +10 per Buy tier;
- all Plant/Wisp Cultivation and Battle base scores;
- Cultivation Support bases/multipliers;
- Battle Round-effect fixed priorities and fallback;
- Battle transition scales/thresholds and any support premiums that are not direct game rules;
- probabilistic willingness percentages for Compost, Mulch, Sunlight, Overgrowth, Pocketed Spark, Buy tendencies, and similar policy gates.

These should later be calibrated against Draw-relative opportunity value and targeted empirical outcomes rather than adjusted ad hoc.

## 12. Inventory conclusions for the next coding step

1. The architecture already has a useful separation between top-level action scoring, card-local scoring, and effect target/branch selection.
2. Explainable score objects and typed selected-choice Chronicle reasoning already exist, so the next observability work should extend them rather than invent a parallel framework.
3. Draw already functions as an implicit comparator, but its numeric scale is still hand-authored and not established as a common utility unit.
4. Card bases are priors, not measured card strengths. Large bases can dominate when contextual intelligence is missing, exactly as the recent learned-policy investigation suggested.
5. The next behavior-changing work should wait until the intended ordinary-human behavior for the first review set is approved.
6. No score constant was tuned in this inventory checkpoint.

## Machine-readable companion

`doc/HUMAN_BASELINE_DECISION_INVENTORY.csv` contains records for strategy interfaces, Human Baseline implementations, card scorers/base values, score/policy constants, and direct test references. It is intended to make later audits/diffs scriptable while this Markdown file remains the human review document.

## Source hygiene observation

The inventory found existing source comments containing legacy checkpoint/planning labels. Per the current project rule, such labels should not remain in production source/docs. They were **not changed here** because this checkpoint explicitly requires no behavior changes and is focused on inventory; a later cleanup can remove them without altering behavior.
