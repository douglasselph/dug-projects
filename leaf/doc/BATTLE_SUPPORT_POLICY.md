# Battle Support Policy Boundary

`BattleSupportPolicy` is the independent high-level policy seam for Battle Step 5.
It is architecture only: the current Human and Mechanical implementations return
exactly the action already selected by their existing `BattleStrategy`, so adding
the seam does not change canonical play.

## Decision owned by this policy

The policy receives every currently legal Step-5 candidate and chooses one:

- a shared Support action (Water, Mulch, Butterfly, Wisp, Worm flip/Heal where represented),
- exact Critter placement (Bee or Worm),
- a Sunlight Support paired with the ordinary Battle Main Action it will fund,
- or a final normal Main Action, which ends that player's Support timing.

Sunlight is therefore represented as a fully formed candidate such as:

- `SUPPORT:SUNLIGHT->DRAW`
- `SUPPORT:SUNLIGHT->PLANT:Flower_11_03`
- `SUPPORT:SUNLIGHT->ROUND_EFFECT:1`

The policy does not execute the action and does not own lower-level effect
parameters. A selected Plant activation still delegates its die/row/branch
targets to the existing effect/Human decision layers.

## Stable identities

Research identities do not use display titles. Examples include:

- `SUPPORT:WATER_REROLL:D8@2`
- `SUPPORT:WATER_REFRESH`
- `SUPPORT:MULCH`
- `SUPPORT:WISP:<card-name>`
- `SUPPORT:CRITTER:BEE:MIDDLE`
- `SUPPORT:SUNLIGHT->PLANT:<stable-card-name>`
- `FINAL_MAIN:DRAW`

Runtime objects are still retained for execution; the string identity exists for
future feature/weight manifests and research output.

## Visible observation seam

`BattleSupportObservation` exposes only information already visible to the
existing Battle decision layer:

- pass number,
- current effective Round card/effects,
- the same player-facing `DecisionContext` supplied to Human Baseline.

No `Game`, hidden deck order, mechanical RNG, or private engine state is exposed.

## Policy composition

`DecisionDirector` now keeps these strategic families independently selectable:

- Buy
- Cultivation Main
- Battle Support
- Battle Main remains part of `BattleStrategy` until its later policy task

`HumanBattleSupportPolicy` and `MechanicalBattleSupportPolicy` are pass-through
implementations. A future learned policy can replace only `battleSupport` with
`DecisionDirector.copy(...)`.

Research CLIs accept `--battle-support-policy human`. `human` is currently the
only accepted value; requesting learned support fails explicitly until that
learner exists.

## Sequencing invariant

The existing Battle coordinator still constructs legality, asks the existing
Battle strategy for its reference Step-5 choice, passes that choice through the
Battle Support policy seam, validates the selected candidate, and executes it.
Therefore:

- Sunlight remains legal only when a legal funded Main exists,
- multiple Sunlight Supports remain possible,
- a Sunlight-funded Main does not consume the final normal Main,
- taking the final normal Main still ends further Support,
- all lower-level targets remain outside this policy.
