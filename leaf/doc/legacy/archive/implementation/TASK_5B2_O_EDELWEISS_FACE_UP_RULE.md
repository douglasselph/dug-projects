# Task 5B.2 — O Edelweiss face-up-only Play rule

Canonical effect: `PLAY_OR_FLIP_ANOTHER_CARD_TWICE`. The superseded
`PLAY_SPENT_OR_FLIP_ANOTHER_CARD_TWICE` and
`PLAY_ANY_OR_FLIP_ANOTHER_CARD_TWICE` research effect codes are retired.

For each of two sequential, optional decisions:

- **Play**: choose another *face-up* grafted Plant with an executable effect.
  Turn it face down (spent), and resolve its effect with normal contextual
  decisions. It remains spent after resolution. It cannot be replayed in the
  second choice without an intervening flip to face up.
- **Flip**: choose another grafted Plant of either facing and invert it.
- **Done**: stop immediately, even if a choice remains.

Recompute legal targets and decision context between choices. The active
Plant-effect chain cannot be targeted (including O Edelweiss itself). Nested
effects use the existing recursion guard and executor.

Example: Two face-up Queen's Blossoms can each be played, drawing four dice
in total and spending both. Two face-down Queen's Blossoms permit flipping
one up and playing it for a total of two drawn dice.

This task corrects execution and first-choice eligibility. Full two-choice
forward projection of Human Baseline remains Task 5B.5. No research CSV,
Plant VP, or AI weights are changed. Previously trained policy results must
be treated as historical under the superseded behavior.
