# Strike Contribution Ledger

Strike resolution now records a compact typed contribution ledger alongside each resolved Strike.

The ledger starts from the final immutable row snapshot and treats each placed die and Critter as a directly measurable contribution. For every contribution it records its current magnitude, whether it contributed non-zero value, whether removing only that contribution changes the Strike winner set, whether removing only that contribution changes the Wound set, and how much Battle VP was awarded to that contribution's player on the row.

`individuallyWinnerDecisive` and `individuallyWoundDecisive` are counterfactual tests: the resolver recomputes the same Strike with exactly that one contribution removed. This deliberately does not claim that every asset on a winning row caused the win.

The ledger is typed data on both `StrikeResolution` and the Chronicle `StrikeResolved` entry. Research code should consume the typed ledger rather than infer contribution from formatted Chronicle text.

## Attribution boundary

This checkpoint attributes final row value to the concrete die or Critter that supplies it. It does not yet claim that an earlier Plant/Wisp effect caused that value. For example, if Root Four More previously raised a die, this ledger sees the die's final value; the later limited-provenance work can connect the relevant portion of that value back to Root Four More. Keeping that boundary explicit avoids false causal claims.

Likewise, `associatedBattleVp` means that the contribution belonged to a player who received that many VP from the Strike. It is not synonymous with individually causing those VP.
