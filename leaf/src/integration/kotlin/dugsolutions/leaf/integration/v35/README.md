# v35 Integration Layer

The integration source set answers one question:

> **When real production components are wired together in a controlled scenario, does the game behave exactly as intended?**

Integration is the bridge between narrow unit tests and statistical simulation research.

## Unit vs Integration vs Simulation

```text
UNIT
Does one rule/helper/strategy make the intended local decision or state change?

        ↓

INTEGRATION
Does the real game reach that situation, provide the correct inputs/legal choices,
execute the result correctly, and record the expected state/events?

        ↓

SIMULATION
What patterns emerge when trusted rules and strategies play many complete games?
```

Integration therefore emphasizes **deterministic scenarios, exact state, invariants, and Chronicle events**. It does not ask whether a strategy produces a statistically good win rate.

## What belongs here

Use `src/integration` when the behavior depends on several real production subsystems working together, for example:

- a complete Cultivation or Battle round;
- exact deck/random/decision setup through the real game graph;
- a card effect that crosses player/Battle/Grove boundaries;
- complete-game lifecycle invariants;
- verifying that a Human Baseline decision is correctly supplied with legal production choices and that its answer is executed correctly.

Do **not** put large matchup studies, win-rate assertions, or 10,000-game experiments here. Those belong in `src/simulation`.

## Structure

```text
chronicle/      Chronicle-specific integration coverage
sanity/         deterministic real-engine scenarios
  setup/
  round/
  cultivation/
  battle/
  decision/
  effect/
  scoring/
  game/
support/        test-only controls and observation helpers
  decision/
  random/
tool/           human-inspection runners such as MechanicalGameSmokeMain
```

Important support types include:

- `IntegrationGameHarness`
- `GameScenario`
- `ScriptedRandomizer`
- `ScriptedDecisionDirector`
- immutable game/player/Grove/Battle snapshots
- Chronicle query/assertion helpers

Scripted decisions should select from the legal choices produced by the real engine rather than constructing fake actions that bypass production validation.

## Deterministic defaults

Integration scripts normally fall back to **Mechanical Control**, not Human Baseline. This keeps correctness tests stable while Human Baseline heuristics evolve during design work.

Use Human Baseline in integration only when the thing being verified is specifically the connection between the production game and Human Baseline behavior.

## Run the integration suite

```bash
./gradlew integrationTest
```

or:

```bash
./gradlew v35IntegrationTest
```

`integrationTest` currently depends on the normal unit-test task.

## Human-readable whole-game inspection

For a complete Mechanical Control game whose Chronicle is written to `output/`, use:

```bash
./gradlew runMechanicalGameSmoke
```

That runner uses integration infrastructure because its purpose is deterministic **engine observation**, not statistical research.

## Related documentation

See:

- [Testing and Verification](../../../../../../../doc/TESTING_AND_VERIFICATION.md)
- [Human Baseline Specification](../../../../../../../doc/HUMAN_BASELINE.md)
- [Code Architecture](../../../../../../../doc/CODE_ARCHITECTURE.md)
- [Chronicle and Output](../../../../../../../doc/CHRONICLE_AND_OUTPUT.md)
