# Human Baseline Policy and Tuning Knobs

This document describes the shared, experiment-friendly policy layer used by the canonical **Human Baseline** strategy.

The implementation is:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/HumanBaselinePolicy.kt
```

The central design rule is:

```text
companion-object default
        ↓
overridable HumanBaselinePolicy method
        ↓
strategy / scorer
```

Production strategy code should not scatter copies of the same threshold or read the companion defaults directly. It should ask the injected `HumanBaselinePolicy` for the behavior it needs.

This gives two useful tuning levels:

1. **Simple experiments** can construct `HumanBaselinePolicy` with different constructor values.
2. **Context-sensitive experiments** can subclass `HumanBaselinePolicy` and override one method without changing production strategy code.

The canonical policy can be supplied through:

```kotlin
DecisionDirector.humanBaseline(policy = ...)
```

so simulation experiments can alter Human Baseline assumptions while leaving the game engine and mechanical RNG untouched.

## 1. Why this layer exists

Milestone-2 review found that several cross-cutting Human Baseline assumptions were starting to live in different places. The clearest example was Critter reserve behavior:

- certified Buy behavior protects 2 Bees and 1 Worm;
- Cultivation purchase-threshold calculations historically counted all Critters as purchasing power;
- generic reserve helpers had separate neutral defaults.

The policy layer separates **what Human Baseline wants** from **the lower-level mathematics used to measure it**.

For example:

```text
HumanBaselinePolicy
    protectedCritterReserve(...)
        ↓
ResourceReserveHeuristics
    computes surplus / deficit / spend penalty
```

`ResourceReserveHeuristics`, `DevelopmentTargetHeuristics`, and similar helpers remain reusable neutral math. `HumanBaselinePolicy` supplies the Human Baseline assumptions that should be shared across decision areas.

## 2. PriorityScore tuning scale

Human Baseline scores are relative preference points, not percentages and not game values. Final scores may exceed 100 after adjustments.

For cross-cutting tuning, use this approximate interpretation:

| Adjustment | Intended meaning |
|---:|---|
| 1–4 | tiny preference / tie influence |
| 5–9 | modest nudge |
| 10–19 | meaningful preference |
| 20–29 | strong preference |
| 30+ | usually decision-dominating |

This scale is intentionally approximate. Its purpose is to make a constant such as `+9` understandable to a human reviewer.

## 3. Current canonical defaults

The current shared defaults are:

| Knob | Default | Intent |
|---|---:|---|
| `DEFAULT_PROTECTED_BEE_RESERVE` | 2 | normally preserve 2 Bees on the way into Battle |
| `DEFAULT_PROTECTED_WORM_RESERVE` | 1 | normally preserve 1 Worm on the way into Battle/Flip flexibility |
| `DEFAULT_PROTECTED_WATER_RESERVE` | 1 | soft Cultivation reserve for Water |
| `DEFAULT_PROTECTED_MULCH_RESERVE` | 1 | soft Cultivation reserve for stored Mulch |
| `DEFAULT_CULTIVATION_DICE_DEFICIT_POINTS_PER_POWER` | 1 | one score point per missing dice-power point |
| `DEFAULT_CULTIVATION_DICE_DEFICIT_MAX_BONUS` | 9 | cap development need at a modest nudge |
| `DEFAULT_CULTIVATION_DONE_SCORE` | 55 | benchmark remaining Support opportunities must beat |
| `DEFAULT_CULTIVATION_RESERVE_SPEND_PENALTY_PER_UNIT` | 15 | meaningful but non-absolute penalty for spending protected Support resources |
| `DEFAULT_BATTLE_TRANSITION_SCALE` | 100 | spacing between named Battle Swing transition tiers |
| `DEFAULT_BATTLE_CLOSE_MARGIN` | 4 | Live-Threat margin defining a close Battle contest |
| `DEFAULT_BATTLE_SECURED_LEAD` | 10 | Live-Threat lead treated as secured-for-now |
| `DEFAULT_BATTLE_HOPELESS_DEFICIT` | 10 | Score-Benchmark deficit treated as potentially hopeless |
| `DEFAULT_BATTLE_MINIMUM_MEANINGFUL_VP_GAIN` | 2 | default VP gate for substantial Support commitment |
| `DEFAULT_BATTLE_WATER_REFRESH_MIN_IMPROVEMENT_STEPS` | 2 | minimum improvement steps for Battle Water Refresh |
| `DEFAULT_BATTLE_IMMEDIATE_RESOLVE_MIN_LEAD` | 5 | minimum lead for the immediate-Strike-resolution Wisp |
| `DEFAULT_BATTLE_IMMEDIATE_RESOLVE_MIN_OPPONENT_SUPPORT` | 3 | minimum live-contender Support capacity for that Wisp |

These values are strategy defaults, not game rules.

Card-specific and action-specific values do **not** belong here. For example, the base score of Queen's Blossom or the low-roll bonus inside `MulchPriority` should remain beside those scorers. `HumanBaselinePolicy` is intentionally limited to assumptions that several decision areas may need to share.

## 4. Protected Critter reserve

The canonical method is:

```kotlin
policy.protectedCritterReserve(context)
```

The default returns:

```text
2 Bees
1 Worm
```

Buy now obtains its reserve from this method rather than from Buy-local constants. `PaymentPriority` does the same.

The `DecisionContext` parameter is deliberate even though the default policy is currently constant. A simulation can override the reserve contextually, for example:

```kotlin
object : HumanBaselinePolicy() {
    override fun protectedCritterReserve(context: DecisionContext) =
        if (context.progress.isFinalCultivationRound) {
            ResourceReserveTargets(bees = 1, worms = 0)
        } else {
            ResourceReserveTargets(bees = 2, worms = 1)
        }
}
```

That experiment changes Human Baseline strategy assumptions only. It does not change the rules of the game.

## 5. Normal purchasing power

The canonical method is:

```kotlin
policy.normalPurchasingPower(context)
```

It represents what Human Baseline should normally think of as spendable during Cultivation:

```text
current Hand dice
+
Bee value above protected Bee reserve
+
Worm value above protected Worm reserve
```

Protected Critters are therefore not casually counted as ordinary purchasing power.

Premium Buy exceptions such as reaching a D20 or cost-17 Flower are intentionally excluded from this calculation. Those are exceptional Buy decisions, not ordinary purchasing power.

B2 passes this value into Cultivation Plant activation scoring and the Compost, Mulch, and Sunlight Round Effect scorers. B6 also injects the same policy into `HumanBaselineEffectStrategy`, so downstream Cultivation die-target choices use the same reserve-aware Buy-threshold assumptions. Their reasoning therefore agrees with certified Buy about which Critters are normally spendable.

## 6. Dice-development nudge

The approved Cultivation principle is:

> Development deficits should be modest nudges, not strong priorities.

The canonical method is:

```kotlin
policy.cultivationDiceDevelopmentBonus(context)
```

The default behavior is:

```text
1 missing dice-power point  → +1
3 missing                   → +3
6 missing                   → +6
9 or more missing           → +9 maximum
```

Because `+9` is the top of the documented **modest nudge** range, development need can break close decisions without making a mediocre Draw or Compost beat an obviously better immediate action.

B2 now applies this bonus to Compost, where the action permanently improves dice-pool power. Draw intentionally does not receive this bonus because it only moves an existing die into Hand and does not improve the long-term dice-power measurement.

## 7. `Done` as a Support benchmark

The approved Support philosophy is:

> Use a Support when it has clear current value; conserve it when the value is marginal. A worthwhile Support may beat `Done` even after the second Main Action.

The canonical method is:

```kotlin
policy.cultivationDoneScore(context)
```

The default is:

```text
55
```

`HumanBaselineCultivationStrategy` now obtains the `Done` score through this method. The default behavior is unchanged from the previous hard-coded value.

Once Support actions receive real action-specific scores, this becomes an intuitive master tuning knob:

```text
lower Done score  → more willing to spend/use Supports
higher Done score → more conservative about Supports
```

For example, an experiment could create:

```kotlin
HumanBaselinePolicy(cultivationDoneScoreValue = 45)
```

without modifying the production Cultivation strategy.

## 8. Reserve-spend penalty

The canonical method is:

```kotlin
policy.cultivationReserveSpendPenaltyPerUnit(context, resource)
```

The initial default is:

```text
15 points per protected unit consumed
```

On the shared tuning scale, `15` is a **meaningful preference**, not an absolute prohibition.

This is intended to support behavior such as:

```text
very valuable Water reroll
    benefit 72
    reserve penalty -15
    effective 57
    Done 55
    → use Water

marginal Water reroll
    benefit 62
    reserve penalty -15
    effective 47
    Done 55
    → conserve Water
```

B4 now defines the initial Cultivation Support reserves explicitly: 1 Water and 1 stored Mulch, while Worm uses the same protected Critter reserve already shared with Buy. `protectedCultivationResourceReserve(context)` exposes the combined targets through an overridable method. These are soft reserves: the scorer applies `cultivationReserveSpendPenaltyPerUnit(...)`, and a strong immediate Support may still spend below them.

## 9. Constructor tuning versus overrides

Use constructor values when an experiment changes a simple global knob:

```kotlin
val policy = HumanBaselinePolicy(
    cultivationDiceDeficitMaxBonusValue = 5,
    cultivationDoneScoreValue = 65
)
```

Use an override when the behavior depends on context:

```kotlin
val policy = object : HumanBaselinePolicy() {
    override fun cultivationDoneScore(context: DecisionContext): Int =
        if (context.progress.isFinalCultivationRound) 45 else 60
}
```

The public factory accepts the policy:

```kotlin
val director = DecisionDirector.humanBaseline(policy = policy)
```

This is the intended seam for future simulation experiments.

## 10. Cultivation A3 decisions captured by this policy

The following designer decisions are now considered approved for the Cultivation behavior contract:

1. **Development deficits are modest nudges, not commands.**
2. **Normal purchasing-power reasoning respects the same 2-Bee/1-Worm protected Critter reserve used by certified Buy.**
3. **The 2-Bee/1-Worm baseline lives in one shared policy location rather than being copied into decision areas.**
4. **Strategies call overridable policy methods rather than directly reading the companion defaults.**
5. **Support resources should be used for clear current value and conserved when marginal.**
6. **Useful Supports may beat `Done` after the second Main Action.**
7. **The willingness to continue using Supports should be tunable through a common `Done` benchmark and reserve-spend penalty.**
8. **Cross-cutting tuning belongs in `HumanBaselinePolicy`; card/action-specific scoring remains local to its scorer.**

This established the shared A4/B1 policy foundation. B2 connected it to threshold-sensitive Cultivation Main Action scorers, and B4 subsequently applied the same policy to action-specific Support scoring.

## 11. Current wiring status

At this checkpoint:

```text
HumanBaselineDecisionDirector
    owns one HumanBaselinePolicy
            ↓
    passes it to Buy
    passes it to Cultivation
```

Buy uses it for:

```text
protected Critter reserve
payment reserve evaluation
```

Cultivation consumes:

```text
normal purchasing power      -> Plant activation, Compost, Mulch, Sunlight, aligned Effect targets
dice-development nudge       -> Compost and qualifying permanent die-development Plants
Done benchmark               -> Done
protected Support reserves   -> Water, Mulch, Worm conservation
reserve-spend penalty        -> Water, Mulch, Worm Support scoring
```

B4 connected the Support layer, and B6 passes the same policy into downstream Effect target/branch decisions where Cultivation action value depends on them. These defaults remain experimental knobs and may be calibrated later without changing the scoring architecture.

## 11. Battle Stage-A policy decisions

Battle designer review (A1–A6) established a second group of cross-cutting Human Baseline knobs. B1 adds the policy seam only; later Battle checkpoints consume these values.

The canonical overridable methods are:

```kotlin
policy.battleTransitionScale(context)
policy.battleCloseMargin(context)
policy.battleSecuredLead(context)
policy.battleHopelessDeficit(context)
policy.battleMinimumMeaningfulVpGain(context)
policy.battleWaterRefreshMinImprovementSteps(context)
policy.battleImmediateResolveMinLead(context)
policy.battleImmediateResolveMinOpponentSupport(context)
```

The defaults are:

```text
transition scale                          100
close Live-Threat margin                   4
secured Live-Threat lead                  10
potentially hopeless Score-Benchmark gap  10
minimum meaningful Strike-VP gain          2
Water Refresh improvement steps            2
immediate-resolve Wisp minimum lead        5
immediate-resolve opponent Support         3
```

These values are strategy assumptions, not rules. The actual Wound margin and legal effect timing remain engine/game-rule facts.

The Battle transition scale intentionally lives in the hundreds. The planned Battle Swing system uses named transitions such as `WIN_FLIPPED`, `TIE_ACHIEVED`, and `WOUND_PREVENTED`, then adds raw margin movement as fine detail. A scale of 100 ensures ordinary die-size movement cannot accidentally reverse the intended transition hierarchy.

The minimum meaningful VP gain is a separate resource-commitment gate. It does not replace Battle Swing. Human Baseline may still make individually useful Wound-prevention or other tactical plays; the +2 VP default primarily answers whether committing several Supports to a longer fight is worthwhile.

The pre-Battle 2-Bee/1-Worm reserve should not be interpreted as a Battle reserve. Its purpose is largely to carry useful Critters into Battle. During Battle, the approved baseline is willing to spend them on the current fight when their Battle use is worthwhile.

The complete approved Battle behavior is documented in [`HUMAN_BASELINE_BATTLE.md`](HUMAN_BASELINE_BATTLE.md), and the implementation sequence is tracked in [`HUMAN_BASELINE_BATTLE_PLAN.md`](HUMAN_BASELINE_BATTLE_PLAN.md).

