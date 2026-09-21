# Human Baseline Cultivation — Target Design Plan

This document describes the **target design** for the Human Baseline Cultivation decision strategy as it moves through Milestone 2 certification.

**Status:** A3 behavior principles approved and A4/B1 shared policy foundation implemented; **not yet a certified Cultivation behavior contract**.

The companion document [`HUMAN_BASELINE_CULTIVATION.md`](HUMAN_BASELINE_CULTIVATION.md) describes the implementation as it exists today. Cross-cutting tuning defaults and override seams are documented in [`HUMAN_BASELINE_POLICY.md`](HUMAN_BASELINE_POLICY.md). This document answers a different question:

> What are we trying to make the Cultivation decision system become?

The primary implementation is:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/
    HumanBaselineCultivationStrategy.kt
```

The overall goal is not to make Human Baseline optimal. It is to make it behave like an ordinary, reasonably competent player who understands the visible value of the choices in front of them without performing deep search, combo planning, or opponent modeling.

## 1. Target mental model

During each Build decision opportunity, Human Baseline should answer one simple question:

> Of the legal things I can do **right now**, which one looks most useful to an ordinary player?

The rules engine remains responsible for legality and for repeatedly asking for another decision until the two required Main Actions have been completed and the player eventually chooses `Done`.

Human Baseline should not recreate game rules. It should compare only the legal choices it receives.

The target flow is:

```text
CultivationBuildCoordinator
    determines legal choices
            ↓
HumanBaselineCultivationStrategy
    evaluates each legal choice on one common PriorityScore scale
            ↓
card/resource/context influences
    make modest, explainable adjustments
            ↓
BaselineScoreEngine
    chooses the highest-scoring legal action
            ↓
StrategyRandomizer
    breaks genuinely equal choices
            ↓
coordinator executes the chosen action
            ↓
if that action needs a target/branch choice,
HumanBaselineEffectStrategy uses the same valuation logic
```

Two architectural principles are especially important:

1. **The top-level action score must reflect the benefit Human Baseline can actually realize.**
2. **If choosing an action assumes a particular good target, the later Effect Choice must use compatible logic and select that kind of target.**

The strategy should never choose Sunlight because one die would gain +3 and then randomly raise a different die that gains only +1. The same principle applies to Compost, Mulch, Plant effects, Wisps, and other target-dependent actions.

## 2. Main Actions — target plan

All legal Main Actions compete on the same understandable scale.

The four broad families are:

```text
Draw
Activate Plant
Round Effect 1
Round Effect 2
```

### 2.1 Draw

Draw should represent the ordinary value of adding the next available die to the current round.

The existing expected-value model is a useful base because the player knows that the lowest-sided die is drawn first. The final behavior may also use a **modest** development adjustment when the player's dice pool is clearly behind expected development.

The target behavior should remain simple. Human Baseline should not simulate every possible roll or future Buy sequence.

Conceptually:

```text
expected value of next die
+ modest dice-development need, if approved
+ obvious immediate context
```

Roll Reward probabilities may be considered only if they materially improve ordinary-human realism without turning Draw into a probability-search system.

### 2.2 Activate Plant

Plant activation should continue to delegate to the card-specific Human Baseline scorer.

Conceptually:

```text
card's ordinary Cultivation value
+ value of what the effect can accomplish right now
+ simple contextual modifiers
```

This allows cards to differ naturally without putting card-name special cases into `HumanBaselineCultivationStrategy`.

The top-level strategy should remain ignorant of most individual card identities. Card-specific knowledge belongs in the card scorer registry.

### 2.3 Compost

Compost should represent the value of a **permanent die upgrade**, balanced against the fact that the upgraded die leaves the current Hand for the rest of this round.

Useful factors include:

```text
size of permanent upgrade
future Cultivation rounds remaining
current-round purchasing power lost by removing the die
whether dice development is behind target, if approved
```

The scorer must evaluate the best real Compost target that Human Baseline could subsequently choose.

### 2.4 Mulch Round Effect

The Mulch Round Effect should favor storing a die whose current roll is poor relative to its future usefulness.

Useful factors include:

```text
low current roll
value/size of the die being preserved
how much Mulch is already prepared
current-round purchasing power lost by removing the die
future usefulness
```

Again, the top-level action score and the later die-target choice must agree.

### 2.5 Water Round Effect

Water should reflect the value of gaining another reusable support resource.

The current 0/1/2+ Water model is a reasonable starting shape, but the exact desired reserve and card interactions remain subject to designer approval.

Owned-card influences such as Root Well should continue to alter the value semantically rather than by hard-coding card names into Cultivation strategy.

### 2.6 Sunlight Round Effect

Sunlight should reflect the **actual useful increase** available from raising a Hand die by +3, including obvious Buy thresholds.

Conceptually:

```text
actual die-value gain
+ meaningful Buy threshold crossed
+ modest contextual adjustment
```

The selected die in `HumanBaselineEffectStrategy` must be the same kind of die whose value justified selecting Sunlight in the first place.

## 3. Support Actions — target plan

The current implementation gives every Support Action the same base score of 30. That is intentionally treated as unfinished Human Baseline behavior.

The target system should score the **specific offered Support Action**, not merely the action type.

The legal Support families currently include:

```text
Play Wisp
Water reroll
Water refresh
Use Mulch
Worm flip
Butterfly reroll
```

### 3.1 Play Wisp

A particular Wisp should use its Human Baseline Wisp scorer rather than receiving a generic Support score.

The decision should account for the immediate usefulness of the Wisp versus the value of retaining an unplayed Wisp for its printed end-game VP where relevant.

The final Wisp valuation belongs in the card/Wisp scorer audit, but Cultivation must at least call the correct scorer.

### 3.2 Water reroll

Water reroll should be based on the expected benefit of rerolling the **specific die** offered in the legal choice.

The strategy should naturally prefer rerolling a bad result over rerolling an already-good result.

The score may also consider whether spending Water would violate the intended reserve policy once that policy is approved.

### 3.3 Water refresh

Water refresh should depend on whether there is meaningful value in refreshing the player's Plant Creature now.

It should not be treated as equally useful in every state merely because Water exists.

The value should come from the actual refresh opportunity and any relevant resource-reserve cost.

### 3.4 Use stored Mulch

Using Mulch should reflect the value of bringing the stored die back into play now.

The specific Mulch token/die matters. A useful high-sided stored die or a die that meaningfully improves the current round should be more attractive than a weak or unnecessary use.

### 3.5 Worm flip

Spending a Worm to flip a Plant should depend on the value of the **specific Plant** being refreshed and on the cost of consuming a Worm that may also matter during Battle.

The later target-selection logic should use the same Plant valuation that justified the Support action.

### 3.6 Butterfly reroll

Butterfly use should reflect the expected keep-best value of rerolling the **specific die** with the **specific available Butterfly**.

Because the separate Butterfly Result strategy chooses whether to keep the original or rerolled result, the action scorer should assume that Human Baseline receives the benefit of that keep-best choice rather than the expectation of a forced reroll.

## 4. `Done` — target plan

`Done` should not mean:

> Always stop immediately after the second Main Action.

Nor should Human Baseline burn every available Support simply because the resource exists.

Instead, after both Main Actions are complete, `Done` should act as the benchmark against which remaining Support opportunities compete.

Conceptually:

```text
If a remaining Support has a clear ordinary-human benefit,
use it.

If the remaining Supports are weak, wasteful, or better saved,
choose Done.
```

A3 approved `Done` as a tunable benchmark. `HumanBaselinePolicy` supplies the default score of 55 through an overridable method. The remaining work is to replace flat Support scores with action-specific benefit so the comparison becomes meaningful.

Before both Main Actions are complete, `Done` is not a legal choice and should not be used as part of strategy policy.

## 5. Shared features — intended use

`BaselineFeatureCalculator` already exposes several common observations:

```text
development targets
resource reserve status
open graft-growth slots
Battle Row Need
```

Not every feature belongs in Cultivation.

The target is to use shared features only where they correspond to an ordinary player's visible judgment.

### Development targets

Plant/dice development may provide a **modest directional adjustment**, for example making Draw or Compost somewhat more attractive when dice development is clearly behind.

It should not override obviously better immediate actions or turn Human Baseline into a development optimizer.

Approved A3 direction: development deficits are **modest nudges, not commands**. The shared `HumanBaselinePolicy` currently defaults to +1 per missing dice-power point capped at +9. Individual scorers have not yet been rewritten to consume that bonus.

### Resource reserves

Cultivation Support scoring should recognize that Water, Mulch, Worms, and other resources can have future value.

A3 approved a shared, overridable policy layer for cross-cutting reserve assumptions. The canonical protected Critter reserve is 2 Bees / 1 Worm. The exact Water/Mulch Support reserve semantics are intentionally still deferred until Support scoring is implemented; they should not be inferred from unrelated legacy defaults.

### Purchase thresholds

Crossing a visible Buy cost threshold is a useful ordinary-human heuristic and should remain part of Cultivation valuation.

However, `PurchaseThresholdHeuristics.purchasingPower()` currently counts all Critters, while the certified Buy strategy normally protects 2 Bees and 1 Worm and makes surplus Critter spending probabilistic.

A3 approved using normal spendable purchasing power: Hand dice plus only Critter value above the shared 2-Bee/1-Worm protected reserve. `HumanBaselinePolicy.normalPurchasingPower(context)` now encapsulates this model. Existing Cultivation scorers still need to be migrated to it during Stage B.

### Graft topology and Row Need

Graft topology is generally a placement concern rather than a top-level Cultivation-action concern, unless a specific card effect makes it directly relevant.

Battle Row Need is not ordinarily relevant during Cultivation and should not be introduced merely because it exists in `BaselineFeatures`.

## 6. Action valuation and Effect targeting must share logic

This is a core target-design requirement.

Several top-level Cultivation actions are valuable only because a particular target exists:

```text
Compost → which die to upgrade?
Mulch → which die to store?
Sunlight → which die to raise?
Water reroll → which offered die?
Water refresh → what useful refresh follows?
Worm flip → which Plant?
Butterfly → which die?
Plant/Wisp effects → effect-specific target/branch choices
```

The current system can sometimes value the best hypothetical target at the top level and then use a more generic Effect Choice scorer later. That can produce internally inconsistent Human Baseline behavior.

The target architecture is to centralize/reuse target evaluation so both layers answer the same question.

A preferred pattern is:

```text
shared evaluator
    scores each legal target
            ↓
Cultivation action scorer
    asks: "What is the best benefit this action can actually realize?"
            ↓
Effect Choice strategy
    asks the same evaluator: "Which legal target realizes that benefit?"
```

The two calls do not necessarily need to persist a target from one hook to the next. They need to use compatible deterministic valuation rules so the later choice realizes the value anticipated by the earlier choice.

## 7. Card influences remain semantic

Owned-card interactions should continue to use semantic `DecisionTag`s and `BaselineInfluencer`s where possible.

For example:

```text
GAIN_WATER_TOKEN
    → ACQUIRE_WATER
    → Root Well can increase its value
```

This is preferred over embedding checks such as:

```text
if player owns Root Well ...
```

inside `HumanBaselineCultivationStrategy`.

The same principle should be used when future cards alter the desirability of a general action/resource category.

## 8. Randomness policy

Human Baseline behavioral randomness must remain independent from mechanical game randomness.

The intended rule is:

```text
mechanical RNG
    dice, deck order, and other game randomness

strategy RNG
    tie-breaking or intentionally probabilistic human behavior
```

Changing a Human Baseline preference or probability must not alter subsequent mechanical rolls or deck outcomes in a reproducible experiment.

For Cultivation, deterministic scores should normally make the choice. `StrategyRandomizer` should be used only for genuine ties or for an explicitly approved behavioral probability.

## 9. Explanation/auditability goal

The eventual certified Cultivation strategy should be easy to inspect in Chronicle decision reasoning.

A useful decision explanation should make it possible to understand something like:

```text
Compost             88
    base             75
    permanent D6→D10 +8
    future rounds     +6
    Buy threshold     -1

Draw D8 expected     53

Water                40

Activate Root Four More 67
```

The precise numbers are illustrative, not approved values.

The important goal is that a human reviewer can see **why** the chosen action beat the alternatives.

## 10. Behavior-contract test plan

Certification should eventually make the unit tests read like an executable description of ordinary Cultivation behavior.

The direct test suite should contain a clearly labeled `Human Baseline Behavior Contract` section covering at least these behavior families:

```text
MAIN ACTIONS
- Draw can win when it is the strongest ordinary option
- valuable Plant activation can beat Draw
- Compost is preferred when the permanent upgrade is clearly strong
- Mulch is attractive for an appropriate poor roll
- Water value falls as the reserve becomes healthy
- Sunlight responds to real +3 value and Buy thresholds

SUPPORTS
- Wisp value depends on the actual Wisp
- Water reroll prefers an actually poor die
- refresh support is used only when useful
- stored Mulch is used when its die has meaningful current value
- Worm flip considers the Plant refreshed and Worm reserve
- Butterfly use reflects keep-best reroll expectation

DONE
- after two Main Actions, a clearly useful Support can beat Done
- weak/wasteful Supports lose to Done

TARGET CONSISTENCY
- Compost chooses the die whose upgrade justified the action
- Mulch chooses the die whose storage justified the action
- Sunlight raises the die whose gain justified the action
- target-dependent Supports make compatible target choices

CROSS-CUTTING
- owned-card influences can change an otherwise close decision
- strategy RNG breaks equal choices without consuming mechanical RNG
```

These are test **categories**, not yet approved numeric thresholds.

Coordinator/integration tests remain responsible for game legality and execution. Human Baseline unit tests are responsible for decision intent.

## 11. Implementation sequence

The planned Cultivation work remains deliberately chunked.

```text
A1 — inventory current strategy, legal choices, tests                 COMPLETE
A2 — inspect shared features and Cultivation scorers                  COMPLETE
A3 — approve ordinary-human behavior contract/direction               COMPLETE
A4/B1 — document decisions + establish shared tuning policy           COMPLETE

B2 — implement/refine Main Action scoring
B3 — implement action-specific Support scoring
B4 — align action scoring with Effect target selection
B5 — build explicit Human Baseline Behavior Contract tests
B6 — focused compile/test/fix until green

C1 — update durable certification documentation
C2 — run full unit + integration + simulation regression
C3 — mark Cultivation CERTIFIED and package final patch
```

The exact B checkpoints may be split further if necessary. Intermediate WIP changes may intentionally be non-compilable as long as that status is explicit and the next checkpoint resolves it.

## 12. Remaining calibration questions

A3/A4 resolved the cross-cutting policy questions: development need is a modest capped nudge; normal purchasing power excludes the protected 2-Bee/1-Worm reserve; `Done` is a tunable Support benchmark; and shared assumptions are accessed through an overridable `HumanBaselinePolicy`.

The remaining questions belong to the Stage-B scorer work rather than to the policy architecture:

- the exact calibrated relationship among Draw, Plant activation, Compost, Mulch, Water, and Sunlight;
- the intended reserve targets for Water, Mulch, and other Support resources where a specific reserve is actually useful;
- the detailed benefit formulas for each Support family;
- whether any Cultivation behavior besides genuine ties should be intentionally probabilistic;
- the exact magnitude of local, action-specific adjustments after focused behavior tests expose where tuning is needed.

These should be resolved in small scorer checkpoints and captured in readable behavior-contract tests. The shared cross-cutting defaults should remain in `HumanBaselinePolicy`; local card/action numbers should remain beside their scorers.
