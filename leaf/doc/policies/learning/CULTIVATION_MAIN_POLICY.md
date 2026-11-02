# Pluggable Cultivation Main Policy

## Purpose

Cultivation Build now has an explicit policy seam for the strategic question:

> Which legal high-level Main Action should this player take?

This is deliberately separate from the broader `CultivationStrategy`, which still
owns optional Support timing and the existing Human Baseline willingness gates.
It is also separate from lower-level effect target selection.

The separation allows a future learned policy to replace only high-level
Cultivation Main selection without changing Buy, Battle, Support, payment, or
card-target logic.

## Current behavior

`DecisionDirector` contains both:

- `cultivation` — existing whole Cultivation action strategy;
- `cultivationMain` — high-level Main Action policy.

The current Human implementation is `HumanCultivationMainPolicy`.

To preserve behavior exactly during this architecture-only step, the existing
`HumanBaselineCultivationStrategy` first reaches the same Main choice it made
before the refactor. That choice is passed to the Main policy as
`referenceAction`. The Human Main policy returns that exact action.

This matters for reproducibility: the refactor does not add another score pass,
consume another strategy-RNG value, or alter Human Baseline's existing Support
versus Main sequencing.

Mechanical Control uses the same pass-through principle through
`MechanicalCultivationMainPolicy`.

A future learned Main policy may replace `referenceAction` with any other action
from the supplied legal Main set. The coordinator validates the replacement.

## Policy boundary

The policy may select among already-legal high-level Main Actions:

- Draw;
- activate a particular legal face-up Plant;
- Round Effect 1;
- Round Effect 2.

`DONE` has a stable machine identity for the broader action vocabulary, but under
current rules Done is legal only after both Main Actions are complete, when there
is no Main Action to select. Completion/Support timing therefore remains with
`CultivationStrategy` in this step.

The Main policy does **not** choose lower-level effect parameters. Examples:

- if it chooses Root Cause, the existing effect strategy still chooses the die;
- if it chooses Mulch, the existing effect strategy still chooses the die;
- if it chooses Compost, the existing effect strategy still chooses the upgrade target.

## Stable action identity

Machine-facing identities are typed and do not use display text:

- `DRAW`
- `PLANT:<PlantCard.name>` such as `PLANT:Root_09_03`
- `ROUND_EFFECT:1`
- `ROUND_EFFECT:2`
- `DONE`

The actual `CultivationMainAction` still carries the physical `CreatureCard`
when a specific Plant activation must execute. Multiple physical copies of the
same authored Plant therefore share a stable card identity while remaining
separate legal action objects.

## Visible observation seam

`CultivationMainObservation` contains only information already exposed to the
player-facing decision layer:

- Main Actions remaining;
- effective Round-card name and effects;
- the same immutable `DecisionContext` used by Human Baseline.

No `Game` object or engine-private hidden state is exposed through the new seam.
The learned feature vector is intentionally deferred to the later learning task.

## CLI status

The Buy training/evaluation research commands accept:

```text
--cultivation-main-policy human
```

`human` is currently the only legal value. This makes the independent policy
selection explicit without pretending a learned implementation exists yet.
Learned weights/training are a later task.

## Scientific intent

The intended modular progression is:

- Buy policy — Human or learned;
- Cultivation Main policy — Human or learned;
- Battle Main policy — later, independently pluggable;
- Battle Support policy — later, independently pluggable.

Do not replace this structure with one monolithic AI player. Independent policy
switching is required so interaction experiments can identify which decision
family discovered a strategy or exploit.
