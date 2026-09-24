# Human Baseline Butterfly Result Contract and Certification Plan

## Status

R4-A and R4-B are complete. Butterfly Result is **NOT CERTIFIED**. R4-C is next.

## Scope

Butterfly Result owns one Human Baseline strategy hook:

```text
chooseButterflyRoll
```

Primary implementation:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/support/HumanBaselineSupportStrategy.kt
```

The decision to **use** a Butterfly belongs to the relevant Cultivation or Battle action strategy. This area owns only the decision made after the die has been rerolled: keep the original face or keep the new face.

## R4-A findings

The current Human Baseline implementation scores both choices from raw face value, so it keeps the higher number and uses `StrategyRandomizer` for an exact tie. That simple behavior matches the designer's intended Human Baseline reasoning.

R4-A also found an engine timing defect. The current Butterfly execution path resolves a Roll Reward from the provisional reroll before Human Baseline chooses whether to keep that reroll. Existing test coverage even preserves the incorrect case in which a rerolled 1 grants a Critter although the player restores the original value.

That is not the game rule.

## Approved game rule: provisional reroll

A Butterfly reroll is provisional until the player chooses which face to keep.

```text
reroll die provisionally
    -> choose ORIGINAL or REROLLED
    -> commit the chosen face
    -> if REROLLED was kept, resolve its Roll Reward normally
    -> if ORIGINAL was kept, resolve no new Roll Reward
    -> finish Butterfly use
```

A rejected reroll of 1 or 2 grants **no** Roll Reward.

Restoring the original face also does **not** grant that face's Roll Reward again. Its original roll happened earlier; Butterfly does not create another roll of the restored value.

If the new roll is kept, normal Roll Reward behavior follows after the choice is committed, including any normal downstream choice or timing required by that reward.

## Approved Human Baseline behavior contract

Human Baseline uses ordinary post-reroll reasoning:

> Keep the higher die value.

This applies in both Cultivation and Battle.

Human Baseline does **not** lower the die deliberately to obtain a Critter or Wisp Roll Reward. Recognizing that a lower 1 or 2 can be strategically preferable because of its reward is explicitly outside Human Baseline and belongs to more advanced strategy.

Human Baseline also does not perform current-row Battle consequence analysis for this post-reroll choice. Although such analysis can be tactically sensible, it is beyond the deliberately simple baseline behavior approved for this hook. The certified Battle analyzers remain responsible for deciding whether using a Butterfly is worthwhile before the reroll; once the Butterfly has been used and both faces are visible, this hook simply retains the higher value.

## Ties and randomness

If original and rerolled values are equal, the choices are strategically equal for Human Baseline. Preserve the existing seeded `StrategyRandomizer` tie mechanism. Do not consume mechanical game RNG to break the strategic tie.

## Explicit non-goals

Butterfly Result must not add:

- deliberate 1/2 retention for Roll Rewards;
- future Buy planning;
- future Battle planning or current-row tactical optimization at this post-reroll hook;
- speculative Wisp identity/value reasoning;
- additional hypothetical rerolls or mechanical RNG consumption;
- duplicated reward resolution inside Human Baseline strategy code.

The strategy chooses a face. The engine owns committing that choice and resolving any Roll Reward earned by a kept new roll.

## R4-C implementation/test target

R4-C should make the smallest production changes necessary to align both the strategy and engine with this contract.

Behavior-contract coverage should demonstrate at least:

- Human Baseline keeps a higher rerolled value;
- Human Baseline restores a higher original value;
- a lower rerolled 1 is rejected even though keeping it would earn a Critter;
- a lower rerolled 2 is rejected even though keeping it would earn a Wisp;
- exact equal faces use seeded strategy tie-breaking;
- a rejected rerolled 1 grants no Critter;
- a rejected rerolled 2 grants no Wisp;
- a kept rerolled 1 resolves its Critter Roll Reward after the keep decision;
- a kept rerolled 2 resolves its Wisp Roll Reward after the keep decision;
- restoring the original face does not re-award a Roll Reward for that old face;
- Roll Reward resolution remains engine-owned rather than strategy-owned.

The existing test that expects a reward from a rejected Butterfly reroll must be corrected because it asserts behavior contrary to the approved game rule.

## Remaining certification sequence

```text
R4-A  current behavior + engine/timing/scorer review          COMPLETE
R4-B  approved durable Butterfly Result contract              COMPLETE
R4-C  engine timing fix + implementation/behavior tests       NEXT
R4-D  focused real-engine integration if a seam warrants it   PENDING
R4-E  docs + full regression + CERTIFY                        PENDING
```

Certification will leave the strategy API count unchanged. When R4-E succeeds, expected Milestone-2 progress is:

```text
7 of 8 major Human Baseline areas certified
10 of 30 strategy hooks certified
```
