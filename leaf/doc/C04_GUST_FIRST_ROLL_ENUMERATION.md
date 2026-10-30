# C04 — Gust first-reroll outcome enumeration

The selected own die rerolls **before** the player chooses the opposing Strike row.
The earlier scorer substituted its average face into the lowest-own-die comparison
and selected a single best row. That loses the conditional nature of eligibility.

Now, for every possible first face from 1 through die sides:

1. Project that actual first face on the actor's row.
2. Recalculate qualifying opposing dice (higher than the actor's lowest die
   in the later-selected row) for each open row.
3. Project the opposing rerolls at fair-face expectation, retaining the existing
   shared multirow Strike-consequence scorer.
4. Keep the best row choice conditional on that first face.
5. Average each face's tactical score with probability 1/sides.

This is an expected *best continuation* rather than the best continuation under
one average roll. It does not consume random numbers or change the real handler.

Limitations: opposing reroll faces are still represented by their means; Gust
reroll-triggered Critter/Wisp reward option value and outcome-dependent later
Support continuation are not included here. `BattleActionAnalysis.vpImpact` remains
an integer diagnostic from one representative branch; its `tacticalValue` is the
outcome-weighted value for action/target ranking. Do not use that diagnostic VP
impact as an expected VP estimate.

The regression covers a three-player, three-row legal-shape board, the same
projected value at Main and target scoring, and filtering to legal die targets.
