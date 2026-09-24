# Human Baseline Wound Resolution Contract and Certification Plan

## Status

R3-A current-behavior/scorer review, R3-B contract, and R3-C implementation alignment with direct behavior-contract tests are complete.

Wound Resolution remains **IMPLEMENTED but NOT CERTIFIED** pending R3-D and R3-E.

## Scope

Wound Resolution owns one Human Baseline strategy hook:

```text
choose
```

Primary implementation:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/wound/HumanBaselineWoundStrategy.kt
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/wound/WoundPriority.kt
```

Relevant shared valuation machinery:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/battle/BattleEnabledPlantAnalyzer.kt
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/card/HumanBaselineCardScorer.kt
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/card/CardScoringHelpers.kt
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/card/PlantPreservationEvaluator.kt
```

The rules engine owns Wound legality. Human Baseline chooses only among the legal `WoundChoice`s supplied by the engine.

## Rules-owned Flip/Snip boundary

Normal Wound resolution does not ask Human Baseline to choose between Flip and Snip.

- If any face-up Plants remain, legal Wound choices are Flips of face-up Plants.
- Only when no face-up Plants remain are Snips offered, and only currently snippable outer Plants are legal.

Therefore the old `WoundPriority` adjustments that rewarded Flip merely for being temporary and penalized Snip merely for being permanent did not represent a real strategic comparison. R3-C removed that misleading cross-operation arithmetic.

## Approved behavior contract

Human Baseline Wound Resolution behaves as follows.

### Flip: sacrifice the least useful immediate Battle contribution

When the legal choices are Flips, evaluate each legal face-up Plant against the **current visible Battle state** and Flip the Plant with the lowest current immediate Battle contribution.

Reuse the certified Battle Plant-opportunity analysis rather than constructing a second approximation in Wound code. `BattleEnabledPlantAnalyzer` already combines the Plant's intrinsic Battle play score with the best visible immediate tactical realization that its effect can make against the current Battle state.

The intended ordinary-human reasoning is:

> If I had the opportunity to use each of these Plants against the Battle as it looks right now, which one would contribute the least? Flip that one.

This is intentionally one-step and current-state based. Do not forecast how the grid may change before the Plant could actually be activated, search opponent responses, or plan a future refresh sequence.

R3-C reuses `BattleEnabledPlantAnalyzer` directly. It does not duplicate or alter the certified Battle effect-analysis logic.

### Snip: sacrifice the least valuable permanent Plant

When the legal choices are Snips, choose the legal outer Plant with the lowest **general preservation value**.

Snip is a permanent loss, so preservation value should represent the Plant as a lasting part of the Creature rather than its immediate contribution to the current Battle grid. The approved ingredients are:

1. **Intrinsic card usefulness.** Reuse the existing card-specific Human Baseline play valuation rather than inventing a second table of Plant quality.
2. **Card cost.** Cost is a broad, context-independent signal of the card's designed power/value tier and should contribute to preservation value.
3. **Projected end-game VP.** Because Snip permanently removes the Plant, current projected VP must contribute materially to preservation value. Existing state-dependent scoring rules such as per-Butterfly, per-grafted-Vine, and per-owned-D4 necessarily use the current `DecisionContext`.

The resulting abstraction should be shared and named for permanent Plant preservation rather than buried as Wound-specific arithmetic. Existing `lossValue` and `CardScoringHelpers.preservationValue` should be reconciled/reused where appropriate instead of adding a third unrelated valuation concept.

R3-C should keep the weighting simple, inspectable, and explainable. It should not tune a complex formula from simulation results. If the existing scorer scales cannot be combined cleanly without a genuine designer judgment, stop and surface that judgment rather than hiding it in arbitrary constants.

### Geometry determines Snip legality, not strategic value

Only outer/snippable cards are candidates because the rules engine supplies only legal Snips. Human Baseline should not add future Creature-topology optimization on top of that legality.

This means lower-cost/lower-value Roots will often naturally be sacrificed before more valuable Plants, but there is no hard-coded "Root first" rule. If a legal outer Root is more valuable than another legal outer Plant under the preservation evaluator, preserve the Root.

### No deliberate Wound-for-Refresh planning

Do not forecast Worm spending, refresh timing, later Battle actions, or deliberately select a Flip because of a planned refresh interaction. Current visible Battle contribution is sufficient for ordinary Human Baseline behavior.

### Ties and randomness

Genuinely equal strategic choices use the existing seeded `StrategyRandomizer` through the common Human Baseline scoring engine. Wound strategy must not consume mechanical game RNG.

## Valuation architecture for R3-C

The intended separation is:

```text
rules engine
    -> legal Wound choices

Flip candidates
    -> shared certified current-Battle Plant opportunity analysis
    -> choose lowest immediate Battle contribution

Snip candidates
    -> shared permanent Plant preservation evaluator
       (intrinsic usefulness + cost + projected VP)
    -> choose lowest preservation value

exact strategic tie
    -> StrategyRandomizer
```

Do not force Flip and Snip through one generic loss formula merely for implementation symmetry. They answer different ordinary-human questions.

## R3-C implementation/test target

R3-C should make the smallest production alignment necessary and add readable behavior-contract tests demonstrating at least:

- strategy chooses only among legal Wound choices supplied by the engine;
- with legal Flip choices, the Plant with the weakest current visible Battle contribution is selected;
- Flip valuation reuses the shared Battle Plant-opportunity analysis rather than a parallel Wound-only Battle evaluator;
- a tactically strong Plant is preserved over a weaker Plant when the current Battle state makes that difference visible;
- with legal Snip choices, the Plant with the lowest permanent preservation value is selected;
- Snip preservation reflects intrinsic usefulness, cost, and projected end-game VP;
- a lower-value legal outer Root may naturally be Snipped before a higher-value outer Plant without a hard-coded Plant-type rule;
- the obsolete `Flip is temporary` / `Snip is a permanent loss` candidate adjustments are removed because the engine does not offer Flip and Snip together;
- no future-grid, opponent-response, refresh-plan, or Creature-topology search is introduced;
- exact strategic ties continue through seeded strategy randomization rather than mechanical RNG.

Keep tests focused on the behavior contract. Do not duplicate the complete certified Battle analyzer test matrix inside Wound tests.

## Remaining certification sequence

```text
R3-A  current behavior + legality/scorer/designer review      COMPLETE
R3-B  approved durable Wound Resolution contract             COMPLETE
R3-C  implementation alignment + behavior-contract tests     NEXT
R3-D  focused real-engine integration if a seam warrants it  PENDING
R3-E  docs + full regression + CERTIFY                       PENDING
```

If the strategy API count remains unchanged, successful R3-E certification moves Milestone 2 to:

```text
6 of 8 major Human Baseline areas certified
9 of 30 strategy hooks certified
```
