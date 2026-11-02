# Human Baseline Graft Placement Contract and Certification Plan

## Status

R2-A and R2-B established the approved behavior contract. R2-C aligned production scoring with that contract and added readable behavior-contract tests. R2-D adds one focused real-engine integration test for the live Buy/Graft placement seam.

Graft Placement is **CERTIFIED**. R2-E completed after the focused R2-C behavior-contract tests and R2-D real-engine integration test were green, followed by a successful full `test integrationTest simulationCheck` regression.

## Scope

Graft Placement owns one Human Baseline strategy hook:

```text
HumanBaselineCreaturePlacementStrategy.choose
```

The rules engine owns placement legality. It supplies the currently legal physical positions for the newly gained Plant card. Human Baseline chooses only among those positions; it does not recreate or weaken the Creature geometry rules.

Primary implementation:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/placement/HumanBaselineCreaturePlacementStrategy.kt
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/placement/GraftPlacementPriority.kt
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/common/GraftTopologyEvaluator.kt
```

Primary direct tests:

```text
src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/placement/HumanBaselineCreaturePlacementStrategyTest.kt
src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/common/GraftTopologyEvaluatorTest.kt
```

## Engine-owned legal geometry

Human Baseline must preserve the engine/strategy boundary reviewed in R2-A:

- Roots use only legal Root positions supplied by the Creature rules.
- Vines use only legal Vine positions supplied by the Creature rules.
- Flowers use only legal Flower positions supplied by the Creature rules.
- Occupied positions remain unavailable.
- LEFT/RIGHT branch identity remains part of the legal position.
- Flowers are endpoints and do not create later Vine growth connectors.
- `GraftTopologyEvaluator` may mirror legal geometry for hypothetical scoring, but strategy code must not become an alternate legality engine.

## Approved behavior contract

Human Baseline Graft Placement behaves as follows.

1. **Legal positions only.** Choose only among positions the engine says are legal.

2. **Do not box the Creature in before the final Cultivation round.** If a placement would unnecessarily consume the final remaining future Vine-growth opportunity while another legal placement preserves growth, strongly prefer the preserving placement. The existing Flower last-slot safeguard expresses the right kind of concern and should remain part of the behavior.

3. **Future-growth value is bounded, not linear.** Human Baseline should notice when the Creature is running out of useful expansion room, but it should not treat every additional theoretical connector as another independently valuable optimization point.

   R2-B defines three ordinary topology bands for R2-C:

   ```text
   BOXED IN       0 future Vine-growth positions
   CONSTRAINED    1-2 future Vine-growth positions
   ADEQUATE       3 or more future Vine-growth positions
   ```

   Prefer a better band when placements differ. Within `ADEQUATE`, five future positions are not strategically better than four merely because the raw count is larger. Within the same band, raw slot count should not continue to accumulate score.

4. **Balance is a weak secondary preference.** When placements are otherwise basically equally useful, prefer the less-developed side of the Plant Creature. This is a modest ordinary-human layout preference, not a reason to sacrifice a better topology band or an important future growth opportunity.

5. **Final Cultivation relaxes future-growth preservation.** On the final Cultivation round, do not preserve expansion capacity solely for a later Cultivation round that cannot occur. The strategy may still use weak balance/tie behavior among otherwise equivalent legal positions.

6. **No invented card-specific placement optimization.** Do not search future Plant sequences or assign special physical-placement value to a particular Plant card unless the card's actual rules make its position strategically relevant.

7. **True ties use strategy RNG.** Genuinely equivalent placements use the seeded `StrategyRandomizer`; placement strategy must not consume mechanical game RNG.

## Priority order

The intended mental model is deliberately small:

```text
legal placement
    ↓
avoid premature final-slot closure
    ↓
prefer a materially safer topology band
    ↓
if basically equally useful, prefer the less-developed side
    ↓
StrategyRandomizer for a genuine tie
```

This is not a long-horizon layout optimizer.

## Policy/tuning direction for R2-C

The topology bands and the relative strength of topology versus balance are Human Baseline strategy assumptions, not game rules. R2-C should keep their tuning values easy to inspect and should follow the existing policy architecture when a value is useful to experiments:

```text
companion/default
        ↓
overridable HumanBaselinePolicy access where appropriate
        ↓
placement scorer/strategy
```

At minimum, the `ADEQUATE` threshold should not be buried as an unexplained magic number in scoring code. R2-C should prefer categorical scores large enough that a weak balance preference cannot overturn a better topology band.

Do not add configuration merely for its own sake: geometry itself remains engine-owned, and card-specific values belong beside the card/scorer that needs them.

## R2-C implementation/test target

R2-C makes the smallest production alignment necessary and adds readable behavior-contract tests demonstrating:

- strategy chooses only from the supplied legal positions;
- before final Cultivation, the scorer applies the strong last-growth-slot safeguard when a Flower consumes that final connector;
- the bounded band classifier distinguishes `BOXED IN` from `CONSTRAINED`;
- `ADEQUATE` beats `CONSTRAINED`;
- additional raw slots within `ADEQUATE` do not create a stronger topology preference;
- weak left/right balance can decide between otherwise equivalent placements;
- weak balance cannot overturn a better topology band;
- final Cultivation does not preserve future growth solely for a nonexistent later Cultivation round;
- exact strategic ties use the seeded strategy randomizer rather than mechanical RNG.

Reuse `GraftTopologyEvaluator` for topology facts instead of duplicating Creature geometry inside the strategy.

R2-C inspection also tightened one expectation from R2-B: with the current Creature geometry, all legal placements for the same Flower consume one currently open Vine connector, so when exactly one connector remains every legal Flower placement consumes it. A same-request choice between a Flower placement that boxes the Creature in and another Flower placement that preserves the final connector does not currently exist. The last-slot penalty is retained because it accurately records the tactical consequence and is shared with card-level Buy reasoning; the placement behavior test therefore verifies the safeguard itself rather than inventing an impossible legal-choice pair.

## R2-D real-engine seam

Inspection found one integration seam worth protecting. Existing `GraftResolverTest` coverage proves legal-placement validation, mutation, face-down grafting, and Chronicle recording, while `CultivationBuySanityTest` proves that a Plant purchase reaches grafting. Those tests use fixed/scripted placement strategies, however, so neither proves that a real Buy passes live legal placements and `DecisionContext` through `GraftResolver` to `HumanBaselineCreaturePlacementStrategy` and commits its answer.

R2-D therefore adds one representative integration scenario. The purchase/payment decision is scripted so the test owns only the placement seam. One Root is pre-grafted on one side; purchasing another Root through the production Buy phase must cause the real Human Baseline placement strategy to choose the opposite, less-developed side when topology is otherwise equivalent. This protects:

```text
production Buy
    -> GraftResolver.prepare
    -> live legal placements + DecisionContext
    -> HumanBaselineCreaturePlacementStrategy
    -> GraftResolver.resolve
    -> committed Creature position
```

Detailed topology bands, policy customization, final-round behavior, legality containment, and strategic tie behavior remain R2-C unit-test responsibilities and are intentionally not duplicated here.

## Remaining certification sequence

```text
R2-A  current behavior + legal geometry/topology review       COMPLETE
R2-B  approved durable behavior/policy contract              COMPLETE
R2-C  implementation alignment + behavior-contract tests     COMPLETE
R2-D  focused real-engine integration if a seam warrants it  COMPLETE
R2-E  docs + full regression + CERTIFY                       COMPLETE
```

R2-E certification leaves the strategy API count unchanged and moves Milestone 2 to:

```text
5 of 8 major Human Baseline areas certified
8 of 30 strategy hooks certified
```
