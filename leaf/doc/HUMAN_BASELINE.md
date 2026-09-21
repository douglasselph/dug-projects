# Human Baseline Specification and Certification Guide

The **Human Baseline** is the canonical Leaf & Let Die simulation control group. It is intended to model **simple, reasonable, experienced-human play** rather than optimal play, deep search, or a deliberately naive engine control.

This document is the navigation and certification entry point for **Milestone 2: verify the Human Baseline**.

The implementation lives in:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/
```

The direct unit tests live in:

```text
src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/
```

The Human Baseline belongs in `src/main`, not `src/simulation`, because it is a canonical reusable decision policy. The simulation layer consumes it through `StrategyProfile.humanBaseline()` and can replace or modify individual decision areas for experiments.

## 1. What exactly counts as a decision point?

There are **three useful levels of counting**.

### Eight major decision areas

`DecisionDirector` composes exactly eight strategy areas:

1. Critter Reward
2. Wound Resolution
3. Graft Placement
4. Cultivation
5. Battle
6. Buy
7. Butterfly Result
8. Effect Choices

These eight areas are the right high-level checklist for Milestone 2. Once each area has been reviewed and certified, every player-choice API currently exposed through `DecisionDirector` has been covered.

### Thirty strategy hooks

The eight areas are **not** eight individual choices. Some areas expose more than one distinct choice method. At the current API boundary there are **30 strategy hooks**:

| Area | Hook count | Player choice hooks |
| --- | ---: | --- |
| Critter Reward | 1 | `chooseCritter` |
| Wound Resolution | 1 | `choose` |
| Graft Placement | 1 | `choose` |
| Cultivation | 1 | `chooseAction` |
| Battle | 3 | `chooseFirstMainAction`, `chooseTurnAction`, `chooseDiePlacement` |
| Buy | 2 | `choosePurchase`, `choosePayment` |
| Butterfly Result | 1 | `chooseButterflyRoll` |
| Effect Choices | 20 | effect target/branch/subset choices listed below |
| **Total** | **30** | |

The 20 effect hooks are:

```text
chooseDie
chooseBattleDie
chooseRootWellBattle
chooseCrossPlayerDieSwap
chooseOptionalDie
chooseDice
chooseDiePair
chooseOptionalDiePair
chooseCritterAndDie
choosePetalToDie4
chooseBeeSource
chooseButterflyTarget
chooseOptionalPlant
chooseOpponentPlantWound
choosePlantEffect
chooseOEdelweiss
chooseWispsToKeep
chooseDieSize
choosePlayer
chooseStrikeRow
```

### Card-specific behavior inside shared hooks

Thirty is the exact count of **strategy API hooks**, not the total number of behavior cases that deserve designer review. A shared hook such as `chooseDie` can be used by several Plant or Wisp effects, and Human Baseline scoring may depend on the specific card/effect.

Therefore Milestone 2 should use:

```text
8 major areas
    ↓
30 strategy hooks
    ↓
card/effect-specific scoring cases where they materially differ
```

This is why the eight-area checklist is useful without pretending the game has only eight literal decisions.

## 2. What is not a Human Baseline decision?

Some game behavior is intentionally mechanical rather than delegated to strategy. Examples include:

- drawing the required lowest-sided die;
- initial Battle ranking from rolled values;
- automatic placement of the original three Battle dice high-to-low;
- resolving Strike winners, VP, and Wounds once choices are complete;
- Doom;
- Cleanup;
- immediate effects that contain no player choice;
- truly random choices required by a rule.

Milestone 2 is concerned with **player choices represented by `DecisionDirector`**, not with reclassifying automatic rules as strategy decisions. Automatic rules still need ordinary unit/integration correctness coverage.

## 3. The eight Human Baseline areas

The following is the certification map. The detailed behavioral contract for each area should eventually be stated both here and in the KDoc at the top of its implementation class. Its direct unit tests should then visibly demonstrate that contract.

### 3.1 Critter Reward

Implementation:

```text
baseline/reward/HumanBaselineRewardStrategy.kt
```

Direct tests:

```text
baseline/reward/HumanBaselineRewardStrategyTest.kt
baseline/reward/CritterRewardPriorityTest.kt
```

Decision hook: choose which legal Critter to gain after a qualifying roll.

**Certification status:** implemented; designer behavior review still required.

### 3.2 Wound Resolution

Implementation:

```text
baseline/wound/HumanBaselineWoundStrategy.kt
```

Direct tests:

```text
baseline/wound/HumanBaselineWoundStrategyTest.kt
baseline/wound/WoundPriorityTest.kt
```

Decision hook: choose among the legal Flip/Snip outcomes already computed by the rules engine.

**Certification status:** implemented; designer behavior review still required.

### 3.3 Graft Placement

Implementation:

```text
baseline/placement/HumanBaselineCreaturePlacementStrategy.kt
```

Direct tests:

```text
baseline/placement/HumanBaselineCreaturePlacementStrategyTest.kt
baseline/common/GraftTopologyEvaluatorTest.kt
```

Decision hook: choose one legal physical graft position for a newly gained Plant card.

**Certification status:** implemented; designer behavior review still required.

### 3.4 Cultivation

Implementation:

```text
baseline/cultivation/HumanBaselineCultivationStrategy.kt
```

Supporting priorities/scorers include Draw, Plant-card, resource, reserve, development-target, and influence logic.

Direct tests begin with:

```text
baseline/cultivation/HumanBaselineCultivationStrategyTest.kt
```

Detailed architecture/review notes:

[`HUMAN_BASELINE_CULTIVATION.md`](HUMAN_BASELINE_CULTIVATION.md)

Decision hook: at each Build opportunity, choose among the currently legal Main Actions, Support Actions, or Done. The rules-engine caller repeatedly asks for another action until Done and contains a repeated-decision-state guard so a legal-but-nonprogressing strategy/executor cycle cannot loop forever.

**Certification status:** implemented; A1 architecture inventory complete; designer behavior review still required.

### 3.5 Battle

Implementation:

```text
baseline/battle/HumanBaselineBattleStrategy.kt
```

Supporting priorities include:

```text
BattleMainPriority.kt
BattleSupportPriority.kt
BattlePlacementPriority.kt
RowNeed.kt
```

Battle has **three** strategy hooks:

1. choose the first Main Action;
2. choose a Support Action or final Main Action on each Step-5 pass;
3. choose the Strike Row for a newly added Battle die.

Direct tests begin with:

```text
baseline/battle/HumanBaselineBattleStrategyTest.kt
```

**Certification status:** implemented; designer behavior review still required.

### 3.6 Buy — CERTIFIED

Implementation:

```text
baseline/buy/HumanBaselineBuyStrategy.kt
```

Supporting priorities include:

```text
PurchasePriority.kt
PaymentPriority.kt
PurchaseThresholdHeuristics.kt
DevelopmentTargetHeuristics.kt
ResourceReserveHeuristics.kt
```

Buy has **two** strategy hooks:

1. `choosePurchase` — choose what to purchase, or choose Done;
2. `choosePayment` — choose the exact dice/Critters used to pay for the selected item.

Direct tests:

```text
baseline/buy/HumanBaselineBuyStrategyTest.kt
baseline/buy/PurchasePriorityTest.kt
baseline/buy/PaymentPriorityTest.kt
```

#### Certified Human Baseline Buy contract

The Buy baseline is intended to model a recognizable ordinary player, not an optimized shopping algorithm. Its accepted behavior is:

1. **Make one principal purchase, then stop.** Human Baseline does not deliberately split purchasing power across several purchases. Multi-buy optimization belongs to more advanced strategy levels.
2. **Consider Plant-vs-die development balance before price.** If Plants are behind their development target while dice are not, prefer a Plant. If dice are behind while Plants are not, prefer a die. If both or neither are behind, do not force a category.
3. **Within the chosen category, buy from the highest affordable cost tier.** This is the core ordinary-player tendency: buy the most expensive thing that fits the chosen development direction.
4. **Use card-specific scoring only inside that highest-cost tier.** Card value can choose between comparable Plant cards, but Human Baseline does not deliberately drop to a cheaper tier for combo/efficiency optimization.
5. **Do not penalize duplicate Plants merely for being duplicates.** A repeated card is evaluated by its actual value, not by a generic diversity preference.
6. **Normally preserve 2 Bees and 1 Worm for Battle.** These are the Buy reserve targets.
7. **Treat surplus Critters as probabilistically available purchasing power.** Let surplus be Bees above 2 plus Worms above 1. With zero surplus the spend chance is 0%. For positive surplus the chance is:

   ```text
   min(100, STARTING_PERCENTAGE + INCREMENT_PER_SURPLUS * surplus)
   ```

   The current constants are `STARTING_PERCENTAGE = 5` and `INCREMENT_PER_SURPLUS = 15`, producing 20%, 35%, 50%, 65%, 80%, 95%, and then 100% as surplus rises from 1 through 7+.
8. **Treat a D20 and a cost-17 Flower as premium thresholds.** Human Baseline may spend Critters from the protected reserve when doing so makes one of those purchases affordable.
9. **Prefer Bees over Worms when a Critter is required and either type can make the payment.** The current tendency is 67% Bee / 33% Worm because Worms retain the additional Flip use.
10. **Use strategy RNG only for Human Baseline probabilities.** Critter-spend and Bee-vs-Worm variation must not consume the mechanical RNG used for dice, decks, or other physical game randomness.
11. **When several legal payments remain, prefer efficient payment.** Minimize overpayment, prefer fewer physical resources, and preserve the Critter reserves where the purchase rules permit it.

The implementation constants are deliberately named and centralized in `HumanBaselineBuyStrategy` so the designer can tune the baseline later without reconstructing the policy from scattered magic numbers.

#### Buy behavior-contract tests

`HumanBaselineBuyStrategyTest` contains a clearly labeled **Human Baseline Behavior Contract** section. Its tests directly demonstrate the rules above, including category balance, highest-cost selection, one-purchase behavior, Critter reserves, the surplus probability curve, premium 17/20 purchases, and both branches of the Bee/Worm preference. Separate implementation/edge-case tests cover fallback and payment mechanics.

To run the Buy certification tests directly:

```bash
./gradlew test --tests 'dugsolutions.leaf.v35.player.decision.baseline.buy.*'
```

**Certification status:** **certified**. The behavioral contract, implementation, readable unit tests, and designer review agree for this area.

### 3.7 Butterfly Result

Implementation:

```text
baseline/support/HumanBaselineSupportStrategy.kt
```

Decision hook: after a Butterfly reroll, choose whether to keep the original or rerolled result.

The decision to **use** a Butterfly is part of the Cultivation or Battle action decision. This strategy area covers only the post-reroll result choice.

Direct tests:

```text
baseline/support/HumanBaselineSupportStrategyTest.kt
```

**Certification status:** implemented; designer behavior review still required.

### 3.8 Effect Choices

Implementation:

```text
baseline/effect/HumanBaselineEffectStrategy.kt
```

This is the largest area because card effects introduce many kinds of targets and branches. The `EffectStrategy` interface currently exposes 20 hooks for choosing dice, Battle dice, optional targets, subsets, pairs, Critter/die combinations, card-specific branches, sources, players, rows, Wisps, and Plant targets.

Direct strategy tests begin with:

```text
baseline/effect/HumanBaselineEffectStrategyTest.kt
```

Card-specific Human Baseline scorers and their tests live under:

```text
baseline/card/
```

**Certification status:** implemented but requires the broadest designer review. Certification must include both the generic hook behavior and important card-specific scoring differences.

## 4. What “certified” should mean

For each of the eight areas, Milestone 2 should not be marked complete merely because code exists or tests pass. Certification should mean all four layers agree:

1. **Behavioral intention** — plain-English description of what a reasonable Human Baseline player should do.
2. **Implementation** — the strategy/priorities actually implement that intention.
3. **Behavior-contract unit tests** — readable tests demonstrate each important rule in the plain-English contract.
4. **Designer inspection** — the behavior has been personally reviewed and accepted as a reasonable baseline approximation.

A recommended structure for every direct strategy test file is:

```text
Human Baseline behavior contract
  - one readable test per stated baseline rule

Supporting / edge-case tests
  - legality fallbacks
  - empty/degenerate cases
  - scoring mechanics
  - regression cases
```

The behavior-contract tests should be readable as a second expression of the specification rather than as implementation archaeology.

## 5. Milestone-2 checklist

Use the eight areas as the high-level completion checklist:

- [ ] Critter Reward certified
- [ ] Wound Resolution certified
- [ ] Graft Placement certified
- [ ] Cultivation certified
- [ ] Battle certified
- [x] Buy certified
- [ ] Butterfly Result certified
- [ ] Effect Choices certified, including material card-specific cases

After all eight are certified, add/run a full **Human Baseline smoke game** with decision reasoning enabled and inspect the Chronicle. That smoke run is a final whole-game confidence check; it does not replace the area-by-area behavior-contract tests.

## 6. Unit, Integration, and Simulation during Milestone 2

For Human Baseline work, use the layers this way:

```text
UNIT
Does the Human Baseline choose what the behavioral contract says it should choose?

        ↓

INTEGRATION
Does the real game reach that decision, provide the correct legal choices/context,
and correctly execute and Chronicle the strategy's answer?

        ↓

SIMULATION
Once the engine and Human Baseline are trusted, what happens statistically over
many complete games?
```

Most Milestone-2 certification work belongs in **unit tests**. Integration tests protect the connection to the real engine. Large simulation experiments should wait until the relevant Human Baseline areas have been certified.

## 7. Related documentation

- [`TESTING_AND_VERIFICATION.md`](TESTING_AND_VERIFICATION.md) — how the three verification/research layers differ and how to run them.
- [`CODE_ARCHITECTURE.md`](CODE_ARCHITECTURE.md) — deeper source-set and decision-system architecture.
- [`USING_THE_SIMULATOR.md`](USING_THE_SIMULATOR.md) — operator workflow for correctness questions and experiments.
- [`CHRONICLE_AND_OUTPUT.md`](CHRONICLE_AND_OUTPUT.md) — inspecting complete-game behavior through Chronicle output.
