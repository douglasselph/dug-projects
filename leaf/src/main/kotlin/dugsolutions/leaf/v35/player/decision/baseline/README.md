# Human Baseline Decision Package

This package contains the canonical **Human Baseline** used by Leaf & Let Die simulation research.

Human Baseline means **simple, reasonable, experienced-human play**. It is intentionally distinct from:

- **Mechanical Control** — legal, deterministic, deliberately naive behavior used for engine control and deterministic tests;
- **Tactical / Strategic / Learned** research strategies — stronger or more specialized behavior developed in the simulation layer.

The Human Baseline implementation lives in `src/main` intentionally. It is a reusable policy consumed by simulations; it is not itself an experiment.

## Start here

The authoritative Milestone-2 navigation/specification document is [Human Baseline Specification](../../../../../../../../../doc/HUMAN_BASELINE.md).

It explains:

- the eight major decision areas;
- the current 30 strategy hooks;
- which implementation and test files belong to each area;
- what designer certification should mean;
- the recommended area-by-area review order.

## Package map

```text
battle/       Battle Main/Support/final-main and Battle die-placement priorities
buy/          purchase and payment decisions
card/         Plant/Wisp-specific Human Baseline scoring
common/       reusable heuristics such as reserves, development targets and Row Need
context/      derived Human Baseline features
cultivation/  Build decision policy
effect/       card-effect target/branch/subset choices
influence/    owned-card/global score influences
placement/    graft-position choice
reward/       Critter Roll Reward choice
scoring/      shared candidate scoring and reasoning
support/      Butterfly original-vs-reroll choice
wound/        Flip/Snip target choice
```

`HumanBaselineDecisionDirector` wires these areas into the eight `DecisionDirector` strategy dimensions and supplies one shared `HumanBaselinePolicy` to decision areas that need cross-cutting tuning. See [`doc/HUMAN_BASELINE_POLICY.md`](../../../../../../../../../doc/HUMAN_BASELINE_POLICY.md).

## Test location

Direct unit tests mirror this package beneath:

```text
src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/
```

During Milestone 2, each major strategy file should gain a concise class-level behavioral contract, and each direct strategy test should contain a clearly identifiable set of tests that demonstrates that contract. Supporting heuristic/edge-case tests can remain separate.

## Certification progress

Milestone 2 is certified one decision area at a time. The authoritative checklist and accepted plain-English contracts live in [`doc/HUMAN_BASELINE.md`](../../../../../../../../../doc/HUMAN_BASELINE.md).

Current status:

```text
Critter Reward       pending designer certification
Wound Resolution     pending designer certification
Graft Placement      pending designer certification
Cultivation          pending designer certification
Battle               pending designer certification
Buy                  CERTIFIED
Butterfly Result     pending designer certification
Effect Choices       pending designer certification
```

For a certified area, the class KDoc, implementation, behavior-contract tests, and the corresponding section of `doc/HUMAN_BASELINE.md` should all describe the same policy.
