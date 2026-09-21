# Human Baseline Cultivation — Architecture Inventory

This document is the detailed navigation and review companion for the Human Baseline Cultivation decision area.

**Milestone-2 status:** under designer review; **not yet certified**.

The purpose of this document is to describe the code as it exists now: who owns the Build loop, what choices are offered to the strategy, how the Human Baseline currently ranks them, which shared features/scorers participate, and what direct tests exist. The companion [`HUMAN_BASELINE_CULTIVATION_PLAN.md`](HUMAN_BASELINE_CULTIVATION_PLAN.md) describes the target design we are working toward. Later Cultivation checkpoints will turn that plan into an approved ordinary-human behavior contract.

The primary implementation is:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/
    HumanBaselineCultivationStrategy.kt
```

The rules-engine caller is:

```text
src/main/kotlin/dugsolutions/leaf/v35/game/round/cultivation/
    CultivationBuildCoordinator.kt
```

The direct Human Baseline tests begin with:

```text
src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/
    HumanBaselineCultivationStrategyTest.kt
```

The Build coordinator itself has broader rule/legality coverage in:

```text
src/test/kotlin/dugsolutions/leaf/v35/game/round/cultivation/
    CultivationBuildCoordinatorTest.kt
```

## 1. Ownership of the Build loop

`CultivationBuildCoordinator.executeActions(...)` owns the Cultivation Build loop. The strategy does **not** decide when it will be asked again.

For each player, the coordinator:

1. tracks how many of the two required Main Actions have been used;
2. rebuilds the currently legal Main and Support choices;
3. adds `Done` only after both Main Actions have been completed;
4. creates a fresh immutable `DecisionContext`;
5. calls `CultivationStrategy.chooseAction(...)`;
6. verifies that the returned action was actually offered;
7. executes that action;
8. loops and asks again until `Done` is selected.

A normal sequence can therefore look like:

```text
2 Main remaining
    Support
2 Main remaining
    Main
1 Main remaining
    Support
1 Main remaining
    Main
0 Main remaining
    Support
0 Main remaining
    Done
```

Support Actions do not consume a Main Action. Any number of currently legal Support Actions can be interleaved before, between, or after the two Main Actions.

### Loop-safety invariant

The caller deliberately does **not** trust a strategy to eventually choose `Done`. It uses two independent safeguards.

First, for each player, `CultivationBuildCoordinator` records every observable decision state it has already presented. That state consists of:

```text
Main Actions remaining
+ currently legal choices
+ current DecisionContext
```

If the same observable state occurs again, the coordinator throws `InvalidGameStateException` instead of calling the strategy again.

This is stronger than merely checking that the returned action is legal. A legal Support Action could otherwise be returned repeatedly if a strategy or executor bug failed to make progress. Reaching the exact same observable decision state means the strategy would be asked the same question again and the Build loop could cycle forever.

The normal engine should continually change at least one observable part of that state:

- a Main Action reduces `mainActionsRemaining`;
- Water is spent;
- Mulch is consumed;
- a Worm is spent and a Plant flips;
- a Butterfly flips face down;
- a played Wisp leaves the player's hand;
- other effect execution changes the visible player/game state;
- `Done` exits the loop.

The repeated-state guard is tested by deliberately simulating a broken Wisp execution that recreates the Wisp it just used. The second identical decision boundary is rejected rather than allowed to loop.

Second, the coordinator enforces a deliberately generous ceiling of **100 decision opportunities per player in one Build**. Normal Build is far below that number. This is a last-resort circuit breaker for a malformed strategy/executor that keeps changing some visible state on every pass and therefore never repeats an identical snapshot. A dedicated test simulates that case by recreating the same Wisp while also changing VP on every resolution; the coordinator still terminates with `InvalidGameStateException`.

## 2. The Cultivation strategy hook

Cultivation currently exposes one Human Baseline strategy hook:

```kotlin
fun chooseAction(
    request: ChooseCultivationActionRequest
): CultivationAction
```

`ChooseCultivationActionRequest` supplies:

```text
roundCard
mainActionsRemaining
legalChoices
context
```

The rules engine determines legality. Human Baseline does not invent actions; it ranks and selects one member of `legalChoices`.

If execution of the selected action later requires another player choice — for example, selecting a die or opponent for a Plant/Wisp effect — that secondary choice belongs to the appropriate Effect Choice strategy hook rather than to `chooseAction` itself.

## 3. Legal choices that can be offered

The current coordinator can offer the following choices.

| Kind | Choice | Offered when |
| --- | --- | --- |
| Main | `Draw` | Dice Supply or Dice Discard contains a die |
| Main | `ActivatePlant(card)` | The Plant is face up and its effect is executable during Cultivation |
| Main | `RoundEffect1` | The first Round Effect is currently executable |
| Main | `RoundEffect2` | The second Round Effect is currently executable |
| Support | `PlayWisp(card)` | Wisp is not immediate, not Battle-only, and its effect is executable |
| Support | `UseWaterReroll(die)` | Player has Water; one choice is generated for each Hand die |
| Support | `UseWaterRefresh` | Player has Water |
| Support | `UseMulch(token)` | One choice is generated for each stored, non-pending Mulch token |
| Support | `UseWormFlip(cardId)` | Player has at least one Worm; one choice is generated per grafted Plant |
| Support | `UseButterfly(butterfly, die)` | One choice is generated for each face-up Butterfly × Hand-die pair |
| Finish | `Done` | Both required Main Actions have been completed |

`Done` is therefore never a legal shortcut around the two required Main Actions.

## 4. Current Human Baseline decision flow

`HumanBaselineCultivationStrategy` converts every legal choice into a `DecisionCandidate` containing:

```text
choice
PriorityScore
tags
```

The candidates are passed to `BaselineScoreEngine`.

At a high level:

```text
legal Cultivation choices
        ↓
base score for each choice
        ↓
semantic tags
        ↓
owned-card influence adjustments
        ↓
highest final PriorityScore
        ↓
strategy-RNG tie break if necessary
```

Human behavioral tie-breaking uses strategy RNG rather than the mechanical game RNG.

A compatibility fallback remains for isolated legacy tests that construct a request with `DecisionContext.EMPTY`; that path delegates to `MechanicalCultivationStrategy`. Production game execution supplies a real context.

## 5. Current Main Action scoring entry points

These values describe the current implementation. They are **not yet the certified Cultivation contract**.

### Draw

`DrawPriority` estimates the expected value of the next lowest-sided die and gives Draw a score approximately equivalent to:

```text
35 + 4 × expected roll
```

This makes drawing a more developed die progressively more attractive.

### Activate Plant

Plant activation does not use one generic fixed score. It delegates to the Human Baseline card scorer for that Plant:

```text
HumanBaselineCardScorerRegistry
    → scorer for this Plant
    → playScore(..., phase = CULTIVATION)
```

This means Plant-specific valuation already participates directly in the Cultivation action comparison.

The card-scoring layer will be inspected more deeply in later checkpoints.

### Round Effect — Compost

`UPGRADE_DIE_FROM_HAND` uses `CompostPriority`.

Its current base score is 75, with contextual adjustments including permanent die development, purchase thresholds, and remaining Cultivation opportunities.

### Round Effect — Mulch

`MULCH_DIE_FROM_HAND` uses `MulchPriority`.

Its current base score is 45, with adjustments related to the selected/available die situation, stored Mulch, die size, and purchasing thresholds.

### Round Effect — Water

`GAIN_WATER_TOKEN` uses `WaterPriority`.

Its current base values are approximately:

```text
0 Water   → 50
1 Water   → 40
2+ Water  → 15
```

Card influences can subsequently change those values.

### Round Effect — Sunlight

`RAISE_DIE_PLUS_3` uses `SunlightPriority`.

Its score depends on the improvement available on current Hand dice and whether the increase crosses useful Buy thresholds.

### Other Round Effects

Any Round Effect not handled by one of the special priority classes currently receives the generic fallback:

```text
45
```

That fallback deserves later designer review rather than being treated as a certified policy.

## 6. Current Support and Done scoring

At the A1 checkpoint, all Support Actions begin with the same base score:

```text
30
```

That currently includes:

```text
Play Wisp
Water reroll
Water refresh
Mulch
Worm flip
Butterfly reroll
```

Semantic tags are attached to several Support choices so owned-card influences can adjust them. Examples include:

```text
PLAY_WISP
SPEND_WATER
REFRESH_CREATURE
SPEND_MULCH
SPEND_WORM
```

However, the base Cultivation strategy itself does not yet directly distinguish whether a particular reroll, refresh, Mulch, Worm target, Butterfly target, or Wisp is especially useful.

After both Main Actions are used, `Done` currently has a base score of 55. Before that point it has a score of 0, although the rules engine should not offer it before both Main Actions are complete.

The current score relationship therefore tends to prefer `Done` over an ordinary Support Action after the second Main Action unless another influence raises the Support choice sufficiently.

Whether that is the desired ordinary-human behavior is intentionally left for later Cultivation review.

## 7. Current direct Human Baseline tests

`HumanBaselineCultivationStrategyTest` currently contains two direct strategy tests:

1. a sufficiently high-value Plant activation can beat an early Draw;
2. owning Root Well can apply enough Water influence to make Water beat a strong Draw.

Those tests prove useful pieces of the scoring architecture, especially that card influences can change the selected action.

They do **not** yet constitute a complete Human Baseline Cultivation behavior contract comparable to the certified Buy tests.

## 8. Coordinator and integration-style coverage

`CultivationBuildCoordinatorTest` provides much broader rules-engine coverage. Among other things, it verifies:

- opening Draw 3;
- exactly two Main Actions;
- `Done` legality;
- Support before/between/after Main Actions;
- Support not consuming a Main Action;
- legal Wisp filtering;
- Water/Mulch/Worm/Butterfly availability;
- pending Mulch exclusion;
- regenerating legal choices after a resource is spent;
- Plant activation and face-down transition;
- repeated use of executable Round Effects;
- rejection of illegal strategy choices;
- deterministic player order;
- Human Baseline completing two Main Actions and then finishing in a simple scenario;
- the repeated-decision-state loop-safety invariant.

The larger deterministic integration suite verifies that real Cultivation actions execute correctly, but most of that coverage uses scripted strategies. It therefore answers a different question from Human Baseline certification:

```text
Coordinator/integration:
Does the engine offer and execute Cultivation choices correctly?

Human Baseline unit contract:
Does an ordinary Human Baseline choose among those legal choices the way we intend?
```

## 9. A1 findings that require later review

The architecture is already cleanly separated: the coordinator owns legality and execution; Human Baseline owns ranking; secondary effect choices belong to their own strategy hooks; tie-breaking uses strategy RNG.

The most important behavior questions exposed by this inventory are:

- the direct Human Baseline test suite is still sparse;
- all Support Actions currently begin at the same base score of 30;
- `Done` currently beats a normal unmodified Support Action 55 to 30 after the two Main Actions;
- the relationship among Draw, Compost, Mulch, Water, Sunlight, and Plant-card play scores needs to be reviewed on one common human-behavior scale;
- Plant activation can contain significant card-specific valuation that must be understood before finalizing the top-level Cultivation hierarchy;
- generic Round Effects currently fall back to 45.

These are observations about the current implementation, not findings that any particular behavior is wrong.

## 10. Planned Cultivation review sequence

The remaining Cultivation certification work is intentionally divided into small checkpoints.

```text
A1 — inventory current strategy, legal choices, tests          COMPLETE
A2 — inspect shared features and Cultivation scorers             COMPLETE
A3 — propose/approve the ordinary-human decision hierarchy       COMPLETE
A4/B1 — document decisions and introduce shared tuning policy      COMPLETE

B2 — apply shared policy to Main Action scoring                  COMPLETE
B3+ — implement Support scoring, target alignment, and tests
       in small checkpoints

C  — documentation, full regression, and certification
```

This document should evolve as those checkpoints are completed. The durable final behavior contract should also be reflected in `HUMAN_BASELINE.md` and in the KDoc on `HumanBaselineCultivationStrategy`.


## Shared tuning policy

Cross-cutting defaults and experiment overrides are documented in [`HUMAN_BASELINE_POLICY.md`](HUMAN_BASELINE_POLICY.md). Cultivation obtains the `Done` benchmark, normal spendable purchasing power, and permanent dice-development nudge through that policy rather than duplicating magic numbers. As of B2, Plant activation plus Compost, Mulch, and Sunlight use policy-based purchasing power; Compost also receives the modest long-term dice-development nudge. Draw intentionally does not receive that nudge because drawing a die does not increase total dice-pool power.
