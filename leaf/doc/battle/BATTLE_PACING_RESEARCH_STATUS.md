> **Historical research record (v37-03):** This document describes the original A/B pacing experiment as of November 2, 2026. Under v38-01, `THREE_ENGAGEMENTS` is adopted by default and the old `CANONICAL` label has been renamed `LEGACY_WHOLE_BATTLE`. See [BATTLE_STRUCTURE_CONFIGURATION.md](BATTLE_STRUCTURE_CONFIGURATION.md) for current routing. The historical settings shown below are not current instructions.

# Leaf & Let Die - Battle Pacing / Three Engagements Research Status

**Status date:** November 2, 2026  
**Rule status:** Experimental only. **Candidate B is not an adopted game rule.**  
**Current default:** `CANONICAL`

This document is the current front door for the Battle pacing experiment. Older `BATTLE_PACING_*`, checkpoint and task documents remain useful implementation/provenance records, but they should not each be treated as separate current plans.

## Why this experiment exists

Blind-playtest feedback identified a real pacing concern: a late Battle could consume a large share of the session, and a player who had finished could spend a long time waiting while others continued Support Actions.

The design problem is therefore not only absolute duration. It is also:

- repeated turn handoffs;
- repeated reassessment of all three Strike rows;
- long passive periods after a player has no meaningful future action;
- whether a structurally different Battle can preserve the valued three-row tactical puzzle while reducing those costs.

The experiment deliberately preserves the three Strike rows. An earlier single-total combat approach showed unwanted runaway behavior and is not the default solution.

## A/B conditions

### A - Canonical v37-03 Battle

Preserve the current rules, including:

- Reveal, Draw & Roll, Rank & Place;
- two normal Main Action tokens per Battle;
- First Main action sequence;
- repeated Support-or-Final-Main opportunities under canonical timing;
- all three Strikes resolve after action play;
- normal VP, Wounds, Doom and Cleanup.

### B - Three Engagements

Resolve High -> Middle -> Low as three sequential Engagements.

For each Strike:

- determine the starting player from the highest currently showing die in that Strike, tied by leftmost Battle Glade column;
- continue clockwise in physical seating order;
- each player receives one opportunity in that Engagement;
- a player may use at most one remaining normal Main Action and up to two Supports, subject to actual rules/timing;
- the Battle still has only **two normal Main Actions total** per player;
- Sunlight-funded Main Actions remain separate from those two normal Main tokens;
- actions may affect the current or later unresolved Strike when otherwise legal;
- already resolved Strike rows are locked and cannot be changed;
- resolve the Strike immediately, including Wounds;
- leave locked dice in place for later Doom;
- after Low resolves, run Doom and Cleanup normally.

The general experimental rule is simple: **once a Strike is resolved, no later effect may change that row.**

## Current implementation state

The source now contains considerably more than the original experiment brief.

### Implemented research infrastructure

The current repository includes work for:

- Engagement starter/order selection;
- paired replay from legal Battle openings;
- executable Three-Engagement Battle flow;
- Main/Support action limits and Sunlight handling;
- locked-row protection and effect-fidelity repairs;
- paired A/B statistics;
- Human-guided Candidate-B policy work;
- Support-continuation parity work;
- targeted natural/heavy-resource scenario cohorts;
- pacing-window instrumentation and modeled passive-downtime analysis;
- Task 7 fairness/counterfactual scaffolding.

The individual implementation records remain under files such as:

- `BATTLE_PACING_CHECKPOINT_1.md`
- `BATTLE_PACING_CHECKPOINT_2.md`
- `BATTLE_PACING_CHECKPOINT_3.md`
- `BATTLE_PACING_POLICY_TASK2.md` through `TASK6.md`
- `TASK7_STATUS.md`

Read them only when working on that implementation detail.

### Task 6

Task 6's documentation records executed acceptance of its final instrumentation patch and validates that the new pacing telemetry did not change canonical Battle outcomes in the deterministic comparison used there.

Its time results remain **model-based proxies**, not measured tabletop minutes. The experiment reported substantially reduced modeled passive waiting in the tested cohort, while tactically consequential observation time could move differently. Human calibration is still required before making real-time claims.

### Task 7

Task 7 adds an optional ordering counterfactual and extra row/player-strength telemetry.

Its own `TASK7_STATUS.md` explicitly states:

- Kotlin compilation/full paired experiments were not completed in that environment;
- a properly conditioned causal A-vs-B last-actor advantage was not established;
- controlled canonical-A ordering and High-row Wound-cascade attribution remained unresolved;
- the B-order rotation is a sensitivity test, not a causal estimate of "being last."

Therefore **Task 7 is not yet a validated acceptance package**.

## Current runtime configuration

The latest source adds a game-level Battle selection separate from Plant/Round resync CSVs:

```text
data/research/resync/game-rules-current.properties
```

Current setting:

```properties
battle.structure=CANONICAL
```

Supported values:

```properties
battle.structure=CANONICAL
battle.structure=THREE_ENGAGEMENTS
```

New `GameConfig` instances read the file by default. An explicitly supplied `battleStructure` overrides it for that game. A setting change affects newly created games, not existing ones.

`THREE_ENGAGEMENTS` requires the simulation runtime provider. Production-only executables must remain canonical and fail clearly rather than silently pretending B is available.

The dedicated A/B runner intentionally selects A and B explicitly and **does not consult this properties file**, preserving experimental independence.

See [`BATTLE_STRUCTURE_CONFIGURATION.md`](BATTLE_STRUCTURE_CONFIGURATION.md).

## What is established vs not established

### Established enough to use as engineering context

- Candidate B can be represented and run in the research layer.
- The three-row lock model and many important interactions have focused tests/repairs.
- Paired A/B data collection exists.
- Human-guided Candidate-B policy parity was improved substantially beyond Mechanical Control.
- Pacing/downtime proxies can be collected without treating JVM CPU time as human time.
- The runtime can now select canonical versus Three Engagements for newly built simulation games.

### Not established as a design conclusion

- Candidate B is not proven faster in real human play.
- Candidate B is not proven free of meaningful last-actor advantage.
- Candidate B is not proven free of High-row Wound snowballing.
- Candidate B is not proven neutral to all Plant/Wisp/Support strategies.
- The two-Support-per-Engagement cap is not proven harmless to resource-heavy final Battles.
- Candidate B has not been adopted as the official rule.

## Next work if Battle pacing becomes the current priority

1. **Validate Task 7 against the current source.** Run focused tests and complete paired 2/3/4-player, Battle 1/2/3 pilots without dropping failures.
2. **Condition fairness analysis properly.** Record starting row lead, dice strength, resources, ready Plants, seat/order and who actually acted last; raw last-actor win rate is not causal evidence.
3. **Add/confirm controlled A comparators.** Where feasible, distinguish rule-system effects from generic order-position effects.
4. **Measure early-Wound cascade.** Quantify whether High-Strike wounds materially depress Middle/Low outcomes and amplify pre-existing advantages.
5. **Stress heavy-resource final Battles.** Include asymmetric Support inventories and resource engines rather than relying only on natural-tail sampling.
6. **Calibrate pacing proxies with human timestamps.** Use real playtest recordings to estimate handoff, execution, reassessment and decision-time coefficients.
7. **Only then make an adoption decision.** Compare pacing benefit against fairness, card/resource distortion, strategic agency and learning burden.

## Experimental integrity requirements

- Do not change canonical cards, dice distributions, player policies or starting resources between paired A/B conditions unless that factor is the explicit subject of study.
- Do not turn one failing sample into a dropped observation; repair the reproducible failure and rerun the cohort.
- Keep canonical mechanics as the default until the user explicitly adopts a rule change.
- Separate simulator-derived statistics, modeled human-time estimates and measured playtest timing.
- Preserve process/replay provenance, seed schedules, output contracts and current launch architecture.

