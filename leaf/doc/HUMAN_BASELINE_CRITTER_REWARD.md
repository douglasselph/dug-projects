# Human Baseline Critter Reward Contract and Certification Plan

## Status

R1-A designer review is complete. R1-B records the approved behavior contract.

Critter Reward is **not yet CERTIFIED**. R1-C implementation alignment and behavior-contract tests, any warranted R1-D integration coverage, and R1-E regression/documentation certification remain.

## Scope

Critter Reward owns one Human Baseline strategy hook:

```text
chooseCritter
```

The engine owns Critter availability and supplies the currently legal choices. Human Baseline chooses only among those legal Critters. This area does not decide when a Critter reward happens.

Primary implementation:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/reward/HumanBaselineRewardStrategy.kt
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/reward/CritterRewardPriority.kt
```

Shared policy:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/HumanBaselinePolicy.kt
```

## Approved behavior contract

Human Baseline Critter Reward behaves as follows.

1. **Legal choices only.** Choose only among Critters currently offered by the engine. If only one Critter is legal, take it.

2. **Fill the protected minimum first.** Use the protected Critter minimum supplied by `HumanBaselinePolicy`. The canonical default is:

   ```text
   2 Bees
   1 Worm
   ```

   If exactly one Critter type is below its protected minimum, prefer that missing type. The minimum is a floor Human Baseline tries to establish, not a requirement to maintain an exact Bee:Worm ratio forever.

3. **Probabilistic preference after the minimum is filled.** Once both protected minimums are satisfied, the otherwise-neutral canonical preference is:

   ```text
   Bee   2/3
   Worm  1/3
   ```

   Each later neutral Critter choice uses that probability independently. Human Baseline does **not** inspect the current Bee:Worm ratio and mechanically correct it back to 2:1. Holdings may naturally drift away from an exact 2:1 ratio.

4. **Visible influences may override the ordinary preference.** Owned-card/global influences such as Bee- or Worm-acquisition synergies remain part of the decision. A sufficiently meaningful visible influence may make one Critter the preferred choice instead of using the ordinary reserve/probability result.

5. **Fresh state for repeated rewards.** If an effect asks for another Critter after one has already been gained, reevaluate from the updated current state. Do not pre-plan a hidden multi-Critter sequence.

6. **Strategy RNG only.** The 2/3-versus-1/3 variation is strategy behavior and must use seeded `StrategyRandomizer`. It must not consume or perturb mechanical game RNG.

7. **No separate early-round Bee bonus.** The previous round-1/round-2 Bee bonus is not part of the approved contract. Filling the default 2-Bee/1-Worm protected minimum already provides the intended early Bee emphasis without a second overlapping rule.

## Policy/configuration contract

The following must be easy to tune globally and override for a particular player/experiment:

```text
protected Bee minimum        default 2
protected Worm minimum       default 1
post-reserve Bee probability default 2/3
```

Follow the existing Human Baseline policy architecture:

```text
companion-object default
        ↓
overridable HumanBaselinePolicy access
        ↓
Critter Reward strategy/scorer
```

The protected Bee/Worm values already exist as `HumanBaselinePolicy` defaults and are exposed through `protectedCritterReserve(context)`. R1-C should reuse that canonical policy rather than copy `2` and `1` into reward code.

R1-C should add the post-reserve Bee probability as a top-level companion default and expose it through overridable policy access. The exact production name/signature should follow the current `HumanBaselinePolicy` conventions.

This permits experiments to provide a custom policy through the existing Human Baseline director/profile wiring so one player can, for example, use a different protected mix or a different post-reserve Bee probability without changing the game engine or the canonical defaults.

## Visible influence boundary

Critter candidates may continue to emit semantic acquisition tags such as Bee/Worm acquisition so `BaselineInfluenceRegistry` can apply visible card/global modifiers.

The influence layer should modify the ordinary Human Baseline preference; it must not infer hidden information or perform future opponent-response analysis.

R1-C should preserve the existing visible influence behavior while replacing the obsolete round-based/count-difference heuristic with the approved policy.

## R1-C implementation/test target

R1-C should make the smallest production alignment necessary and add readable behavior-contract unit tests demonstrating at least:

- missing Bee below the protected minimum is preferred;
- missing Worm below the protected minimum is preferred;
- only one legal Critter is taken;
- once both minimums are satisfied, seeded strategy randomization can select Bee or Worm according to the configurable probability boundary;
- changing the policy for one Human Baseline player changes its protected minimum and/or probability without changing global defaults;
- visible Bee/Worm acquisition influences can override the ordinary neutral preference where their configured score effect is sufficient;
- mechanical RNG is not consumed by the Critter strategy choice.

Do not broaden R1-C into unrelated scorer cleanup.

## Remaining certification sequence

```text
R1-A  current behavior + engine/legal-choice inventory       COMPLETE
R1-B  approved durable behavior/policy contract              COMPLETE
R1-C  implementation alignment + behavior-contract tests     NEXT
R1-D  focused real-engine integration if inspection warrants PENDING
R1-E  docs + full regression + CERTIFY                       PENDING
```

If the strategy API remains unchanged, certification of Critter Reward will move Milestone 2 to:

```text
4 of 8 major Human Baseline areas certified
7 of 30 strategy hooks certified
```
