# Human Baseline Cultivation — Target Design Plan

This document describes the **target design** for the Human Baseline Cultivation decision strategy as it moves through Milestone 2 certification.

**Status:** **CERTIFIED.** Stage B completed through B7, focused production/test verification passed, and Stage C durable documentation plus full unit + integration + simulation regression are complete.

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

The existing expected-value model remains the base because the player knows that the lowest-sided die is drawn first. B2 deliberately does **not** add the long-term dice-development deficit bonus to Draw: the shared `dicePower` target measures permanent pool strength, while Draw only moves an existing die from Supply/Discard into Hand for this round.

The target behavior should remain simple. Human Baseline should not simulate every possible roll or future Buy sequence.

Conceptually:

```text
expected value of next die
+ obvious immediate context
```

If a future experiment wants a separate notion of *current-round die availability need*, that should be modeled as its own explicit heuristic rather than reusing the permanent dice-development deficit.

Roll Reward probabilities may be considered only if they materially improve ordinary-human realism without turning Draw into a probability-search system.

### 2.2 Activate Plant

Plant activation delegates card-local judgment to the card-specific Human Baseline scorer and B3 now makes the comparison boundary explicit through `PlantActivationPriority`.

Conceptually:

```text
card's ordinary Cultivation value
+ value of what the effect can accomplish right now
+ obvious Buy-threshold/context modifiers
+ modest cross-cutting development nudge only when the effect
  unambiguously improves permanent dice-pool strength
```

This allows cards to differ naturally without putting card-name special cases into `HumanBaselineCultivationStrategy`.

The top-level strategy remains ignorant of individual card identities. Card-specific knowledge belongs in the card scorer registry; cross-cutting Cultivation policy belongs in `PlantActivationPriority`/`HumanBaselinePolicy`.

B3 deliberately gives no generic Plant-development bonus for activation. Reusing an already grafted Plant does not increase Plant count. It also limits the permanent dice-development nudge to effects whose permanent improvement is unconditional. B6 now aligns the Cultivation target/branch choices that directly support activation valuation; broader Effect-choice behavior remains a separate Milestone-2 decision area and later card/Wisp scorer audit.

The direct comparison tests now demonstrate both directions: a strong visible Plant opportunity can beat Draw, while a weak one can lose, and the same Plant can switch between those outcomes as the current state changes.

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

B4 replaces the earlier flat Support score with action-specific valuation through `CultivationSupportPriority`.

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

A particular Wisp now uses its Human Baseline Wisp scorer rather than receiving a generic Support score.

The decision should account for the immediate usefulness of the Wisp versus the value of retaining an unplayed Wisp for its printed end-game VP where relevant.

The final Wisp valuation belongs in the card/Wisp scorer audit, but Cultivation must at least call the correct scorer.

### 3.2 Water reroll

Water reroll should be based on the expected benefit of rerolling the **specific die** offered in the legal choice.

The strategy should naturally prefer rerolling a bad result over rerolling an already-good result.

The score also considers whether spending Water would cross the shared protected reserve, applying the policy's soft reserve-spend penalty rather than a hard prohibition.

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

A3 approved `Done` as a tunable benchmark. `HumanBaselinePolicy` supplies the default score of 55 through an overridable method. B4 now gives each Support family an action-specific benefit score, so useful Supports can beat `Done` while marginal ones can be conserved.

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

Plant/dice development may provide a **modest directional adjustment** only where the action actually changes the measured long-term development.

It should not override obviously better immediate actions or turn Human Baseline into a development optimizer.

Approved A3 direction: development deficits are **modest nudges, not commands**. The shared `HumanBaselinePolicy` defaults to +1 per missing dice-power point capped at +9. B2 applies that nudge to Compost because Compost permanently increases dice-pool power. It does not apply the same nudge to Draw because Draw does not change total dice-pool power.

### Resource reserves

Cultivation Support scoring should recognize that Water, Mulch, Worms, and other resources can have future value.

B4 completes the initial Cultivation Support reserve semantics through the shared policy. The canonical protected Critter reserve remains 2 Bees / 1 Worm, while Cultivation also begins with a soft reserve of 1 Water and 1 stored Mulch. These are tunable defaults, not hard prohibitions: spending below reserve applies a score penalty and a sufficiently valuable Support can still win.

### Purchase thresholds

Crossing a visible Buy cost threshold is a useful ordinary-human heuristic and should remain part of Cultivation valuation.

The original `PurchaseThresholdHeuristics.purchasingPower()` model counted all Critters, while certified Buy protects 2 Bees and 1 Worm and makes surplus Critter spending probabilistic.

A3 approved using normal spendable purchasing power: Hand dice plus only Critter value above the shared 2-Bee/1-Worm protected reserve. `HumanBaselinePolicy.normalPurchasingPower(context)` encapsulates this model. B2 now supplies that policy value to Plant activation scoring and to the Compost, Mulch, and Sunlight Round Effect scorers, so their Cultivation Buy-threshold calculations no longer treat protected Critters as ordinary spending power.

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

B6 implements this requirement for the Cultivation cases that had exposed the mismatch. Compost, Mulch, and Sunlight now expose target-specific scorers used by both the top-level Round Effect valuation and `HumanBaselineEffectStrategy`. This also fixed an important rules mismatch in the old Compost scorer: it could value a later available die size as though a normal Upgrade could skip a missing intermediate size, while the real Upgrade rules require the exact next normal step.

Cultivation die-targeting Plant/Wisp effects now pass the same `HumanBaselinePolicy.normalPurchasingPower(context)` into downstream target scoring that was used when their activation value was calculated. `RAISE_DIE_PLUS_3` is explicitly target-scored as well. Petal To Die 4 now shares one branch evaluator between top-level card valuation and the later `choosePetalToDie4` branch decision.

The architecture centralizes/reuses target evaluation so both layers answer the same question.

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

The certified Cultivation strategy should remain easy to inspect in Chronicle decision reasoning.

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

## 10. Behavior-contract tests

B5 makes the direct strategy tests read like an executable description of ordinary Cultivation behavior, following the same certification pattern already used by Buy.

`HumanBaselineCultivationStrategyTest` now contains a clearly labeled nested `Human Baseline Behavior Contract` section. Its tests assert externally visible choices rather than exact internal score arithmetic. Lower-level priority/policy tests retain responsibility for scorer math and wiring details.

The broader certification matrix remains:

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

The B5 top-level contract directly covers the core Main/Support/Done behavior in this matrix. B6 adds focused target/branch tests proving that the later Effect strategy realizes the target value anticipated by Compost, Mulch, Sunlight, and Petal To Die 4. Detailed Wisp/refresh/Mulch/Worm/Butterfly arithmetic remains in lower-level scorer tests and the later card/Wisp audit.

Coordinator/integration tests remain responsible for game legality and execution. Human Baseline unit tests are responsible for decision intent.

## 11. Implementation sequence

The Cultivation work was deliberately chunked as follows; all certification checkpoints are now complete.

```text
A1 — inventory current strategy, legal choices, tests                 COMPLETE
A2 — inspect shared features and Cultivation scorers                  COMPLETE
A3 — approve ordinary-human behavior contract/direction               COMPLETE
A4/B1 — document decisions + establish shared tuning policy           COMPLETE

B2 — implement/refine Main Action scoring                              COMPLETE
B3 — make Plant activation comparison explicit                         COMPLETE
B4 — implement action-specific Support scoring                         COMPLETE
B5 — build explicit Human Baseline Behavior Contract tests               COMPLETE
B6 — align action scoring with Effect target/branch selection                 COMPLETE
B7 — focused compile/test/fix until green                              COMPLETE

C1 — update durable certification documentation                          COMPLETE
C2 — run full unit + integration + simulation regression                  COMPLETE
C3 — mark Cultivation CERTIFIED and package final patch                    COMPLETE
```

This sequence is retained as a record of how the area was certified. The same checkpoint discipline should be reused for other large Human Baseline areas.

### B7 focused verification result

B7 required no additional behavior fixes. The latest source compiled successfully through Gradle for both production and test Kotlin, and the focused Cultivation/Human-Baseline test selection completed successfully:

```text
57 tests started
57 tests successful
0 failed
```

The focused selection covers the Cultivation behavior contract, Main and Support priorities, Plant activation comparison, Cultivation Effect target/branch alignment, shared policy wiring, Human Baseline director wiring, and the Cultivation Build coordinator safety/legality tests.

The compile still reports the previously known unused `cardName` warning in `CardScoringHelpers`, and the test compile reports previously known deprecation warnings around the older `Baseline...` aliases. Those are not Cultivation B7 failures and are intentionally left outside this checkpoint.

### Stage C certification result

The final full-project verification completed successfully with:

```bash
./gradlew --offline --no-daemon test integrationTest simulationCheck
```

Result: `BUILD SUCCESSFUL`; unit, integration, and simulation-layer verification all passed. With durable documentation updated and the designer-approved behavior contract represented in code/tests, Cultivation is now **CERTIFIED** for Milestone 2.

## 12. Remaining calibration questions

A3/A4 resolved the cross-cutting policy questions: development need is a modest capped nudge; normal purchasing power excludes the protected 2-Bee/1-Worm reserve; `Done` is a tunable Support benchmark; and shared assumptions are accessed through an overridable `HumanBaselinePolicy`.

B2 wired the approved cross-cutting policy into the threshold-sensitive Main Actions, B3 made Plant activation comparison explicit through `PlantActivationPriority`, and B6 now keeps target/branch execution aligned with the value anticipated by the top-level action. The remaining questions are calibration questions rather than architectural gaps:

- the exact calibrated relationship among Draw, Plant activation, Compost, Mulch, Water, and Sunlight;
- later calibration of the B4 Water/Mulch/Worm/Butterfly/Wisp formulas if simulation evidence shows they are too conservative or too liberal;
- whether any Cultivation behavior besides genuine ties should be intentionally probabilistic;
- the exact magnitude of local, action-specific adjustments after focused behavior tests expose where tuning is needed.

These should be resolved in small scorer checkpoints and captured in readable behavior-contract tests. The shared cross-cutting defaults should remain in `HumanBaselinePolicy`; local card/action numbers should remain beside their scorers.
