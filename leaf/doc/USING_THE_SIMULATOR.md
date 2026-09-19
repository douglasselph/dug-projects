# Using the Simulator

This document is the practical entry point for using the Leaf & Let Die codebase as a game-design research tool.

## 1. Decide what kind of question you are asking

Before writing code, classify the question.

### A. Is a rule or subsystem implemented correctly?

Use a **unit test** or an **integration test**.

Examples:

- Does `DoomResolver` remove the correct dice?
- Does a Wound flip or Snip the correct kind of Plant card?
- Does a complete Cultivation round correctly move dice through Supply, Hand, and Discard?
- Does Wispquake reroll exactly the dice it should?

### B. Does the whole engine behave correctly in a specific constructed situation?

Use an **integration test** under `src/integration`.

Examples:

- Give Player 1 exact dice, exact decisions, and an exact Round Card, run a real round, and inspect the resulting state.
- Swap Battle positions while keeping the rest of the state identical.
- Force a particular card effect and verify Chronicle entries and resulting player/Grove state.

### C. Is a game-design pattern too strong, too weak, too lucky, or exploitable?

Use a **simulation experiment** under `src/simulation` with supporting tests under `src/simulationTest`.

Examples:

- Does prioritizing bigger dice outperform normal play?
- Does targeting a particular Plant card improve win share?
- Does an early abundance of Wisps produce an excessive win-rate advantage?
- Does seat position matter after controlling for mechanical randomness?
- How often can the player who is last after the first Battle still win?

This distinction is important: **integration tests establish correctness; simulations establish tendencies.** A simulation result is not trustworthy if the underlying engine path is not already covered by correctness checks.

---

## 2. The four source sets

The repository currently has four important source areas.

### `src/main`

The production rules engine.

It contains the actual Leaf & Let Die model and mechanics: game setup, rounds, dice, players, Grove, Plant/Wisp/Round cards, effects, Battle, scoring, Chronicle, decisions, and the canonical strategy implementations that are part of the engine boundary.

Do not put experiment-specific scaffolding here unless it represents a reusable engine capability.

### `src/test`

Fast, isolated unit tests for `src/main`.

Use this when the thing being tested can be sensibly isolated. Unit tests should usually be the first line of defense for individual classes, converters, effect handlers, scoring primitives, and rules helpers.

Run with:

```bash
./gradlew test
```

### `src/integration`

Deterministic **real-engine scenario tests**.

Integration code constructs the real production graph but provides test-only controls such as scripted random results, scripted decisions, exact deck setup, snapshots, and Chronicle assertions.

The goal is:

> Construct a known game state, execute real game behavior, and verify exactly what happened.

Integration is not the place for 10,000-game statistical research. It is the place to prove that the experiment machinery and rules engine do what the simulation later assumes they do.

Run with:

```bash
./gradlew integrationTest
```

`v35IntegrationTest` currently provides the same v35 integration-test target as an alternate task name.

### `src/simulation`

The research/application layer that consumes the rules engine.

This is where strategy profiles, experiment definitions, batch concepts, focused-card studies, tournament/matchup structures, analysis summaries, and future learning tools belong.

Simulation code must not depend on integration-only helpers. The simulation layer should use production APIs and research-specific abstractions suitable for thousands of games.

Supporting tests for this source set live in `src/simulationTest`.

Run them with:

```bash
./gradlew simulationTest
```

or compile and test the simulation layer together with:

```bash
./gradlew simulationCheck
```

---

## 3. How to browse what already exists

### Integration scenarios

Start here:

```text
src/integration/kotlin/dugsolutions/leaf/integration/v35/
```

The current tests are grouped by game responsibility:

```text
sanity/
  setup/
  round/
  cultivation/
  battle/
  decision/
  effect/
  scoring/
  game/
chronicle/
support/
```

Good examples to read first:

- `sanity/setup/IntegrationHarnessSmokeTest.kt` — minimal example of building a real game through the integration harness.
- `sanity/cultivation/CompleteCultivationRoundSanityTest.kt` — example of exact scripted decisions + scripted rolls + snapshot/Chronicle assertions.
- `sanity/battle/CompleteBattleRoundSanityTest.kt` — corresponding full Battle example.
- `sanity/effect/CardEffectContractTest.kt` — broad coverage that loaded effects have valid execution paths.
- `sanity/game/WholeGameSanityTest.kt` — complete-game invariants.

The reusable test controls are under:

```text
src/integration/kotlin/dugsolutions/leaf/integration/v35/support/
```

Important helpers include:

- `IntegrationGameHarness`
- `GameScenario`
- `GameSnapshot`, `PlayerSnapshot`, `GroveSnapshot`, `BattleSnapshot`
- `ScriptedRandomizer`
- `ScriptedDecisionDirector` and its decision-area strategies
- `ChronicleQueries` and `ChronicleAssertions`

### Simulation research

Start here:

```text
src/simulation/kotlin/dugsolutions/leaf/simulation/v35/
```

Key areas:

```text
experiment/       experiment definitions and batch abstractions
analysis/         result summaries/analysis types
strategy/         named strategy profiles and strategy families
learning/         future feature/model/training/persistence boundary
```

A concrete experiment already implemented is the focused-card harness:

```text
experiment/card/CardFocusExperiment.kt
```

It runs one Planned Baseline player against Human Baseline opponents, rotates the focused player through every seat, reuses the same sample seed for each seat rotation, and reports aggregate card and game metrics.

Its support tests live here:

```text
src/simulationTest/kotlin/dugsolutions/leaf/simulation/v35/
```

---

## 4. First visual sanity check: run one complete game

Before doing statistical research, it is useful to simply watch the engine complete a game. The project now has a dedicated human-inspection smoke run:

```bash
./gradlew runMechanicalGameSmoke
```

It constructs a real four-player game through `IntegrationGameHarness`, uses the normal first-game Grove, and leaves every player's decision factory unspecified. `GameScenario` therefore supplies **Mechanical Control** to all four players. Mechanical Control is legal and deterministic but deliberately naive; the point of this run is not to judge strategy quality.

The run should terminate normally and write:

```text
output/
  smoke/
    mechanical-control/
      seed-13579/
        summary.txt
        chronicle.txt
```

`summary.txt` shows the setup, number of rounds completed, number of Chronicle entries, final scores, and winner(s). `chronicle.txt` is the chronological record of what the engine actually did: rolls, rewards, Main Actions, Support Actions, effects, purchases, grafts, Battle order, Strikes, Wounds, Doom, Cleanup, and final scoring.

To rerun the same mechanical game, use the same seed. To inspect a different deterministic game:

```bash
./gradlew runMechanicalGameSmoke -PsmokeSeed=24680
```

This run complements, rather than replaces, `WholeGameSanityTest`. The test automatically asserts lifecycle invariants; the smoke runner gives a human a file to read.

For the output convention and Chronicle design, see [Chronicle and Output](CHRONICLE_AND_OUTPUT.md).

---

## 5. Running a focused-card experiment

`CardFocusExperiment` is the best current example of how a statistical experiment should be structured.

Conceptually:

```kotlin
val result = CardFocusExperiment(
    gameFactory = gameFactory,
    gameRunner = gameRunner
).run(
    spec = CardFocusExperimentSpec(
        targetCard = TargetCard("Root_07_02"),
        targetCount = 2,
        numPlayers = 4,
        gamesPerSeat = 1_000,
        baseSeed = 10_000L
    ),
    selectedPlantCards = selectedPlantCards
)

println(CardExperimentReport.render(result))
```

With four players and `gamesPerSeat = 1_000`, this means 4,000 completed games because the focused player occupies each seat equally.

The experiment reports values such as:

- focused winner rate and win share;
- baseline comparison win share;
- average VP and VP delta;
- target-card acquisition frequency;
- copies purchased and surviving;
- target-card activations;
- target Plant VP;
- Battle Strike VP;
- per-seat results.

That pattern should be reused for future experiments: **define the intervention, control confounders, repeat many games, aggregate the result.**

---

## 6. Adding a new integration scenario

Use an integration test when the expected outcome can be stated exactly.

A typical workflow is:

1. Choose the closest existing test class and copy its structure.
2. Build a `GameScenario` / specialized harness setup.
3. Use `ScriptedRandomizer` when exact random outcomes matter.
4. Use `ScriptedDecisionDirector` to select exact legal decisions.
5. Execute one round, action sequence, or game through the real engine.
6. Assert the resulting snapshot.
7. Assert important typed Chronicle entries.
8. Assert that scripts/random results were exhausted when exact call counts matter.

Prefer typed state and Chronicle assertions over comparing human-readable strings.

When the test requires a new controllability seam, first ask whether that seam is generally useful and belongs in production (for example, independent RNG streams) or is purely a test helper and belongs under `src/integration`.

---

## 7. Adding a new simulation experiment

Use this pattern.

### Step 1 — State the hypothesis before coding

Bad:

> See what happens with Wisps.

Better:

> If one player receives an unusually large number of early Wisp rewards while all later play is normal, does that player's win share increase enough to suggest an exploitable or overly swingy advantage?

### Step 2 — Define the intervention

Only change what is necessary to create the condition being studied.

Examples:

- one strategy modifier;
- one card forced into/out of the Grove;
- one player's first two rounds of die results;
- one Battle seat/column assignment;
- one resource grant.

Everything else should remain as close to the control condition as possible.

### Step 3 — Decide what must be held constant

Useful controls include:

- mechanical seed;
- strategy tie seed;
- Grove composition;
- Round Deck;
- strategy family;
- player count;
- seat rotation;
- number of games.

The codebase deliberately separates mechanical and strategy randomness so these can be varied independently.

### Step 4 — Run both control and intervention samples

Whenever possible, use paired or matched samples rather than comparing two unrelated bags of games.

### Step 5 — Record metrics that answer the hypothesis

Do not collect only win rate. Often the pathway matters as much as the endpoint.

For example, a Wisp experiment should probably include:

- win share / winner rate;
- final VP;
- Wisps gained, played, Trashed, and retained at game end;
- VP from retained Wisps;
- Butterfly gains caused by Wisps;
- immediate VP/effect value attributable to Wisps;
- whether Wisp Reckoning occurred and what it forced;
- Battle VP and Wounds, to see whether the advantage propagates into combat;
- seat breakdown.

### Step 6 — Add simulation-layer tests

The statistical experiment itself may be expensive, but its configuration, aggregation, pairing/rotation logic, and reporting should have fast `src/simulationTest` coverage.

### Step 7 — Keep the result reproducible

Every report should preserve enough configuration information to rerun the same experiment: strategy definitions, player count, game/seat counts, seeds, Grove selection, Round setup, and the exact intervention.

---

## 8. What belongs in integration versus simulation?

A useful rule of thumb:

| Question | Use |
| --- | --- |
| “If the next D4 is scripted to roll 2, is a Wisp actually gained?” | Integration |
| “If Wispquake fires, are the correct dice rerolled?” | Integration |
| “If one player gains many early Wisps, how much does their win rate change?” | Simulation |
| “Does Player 1's scripted purchase really buy the requested card?” | Integration |
| “Does prioritizing this card across thousands of games create a dominant strategy?” | Simulation |
| “Does the full game complete with legal state and scoring?” | Integration |
| “How often is the eventual winner already leading after Battle 1?” | Simulation |

Many good simulation projects require **both**: first an integration test proving the intervention is implemented correctly, then the large experiment measuring its consequence.

---

## 9. Reading results responsibly

A large number of games can give a precise answer to the wrong question. Before interpreting an observed advantage, check for confounders:

- seat imbalance;
- different mechanical random streams;
- different Grove or Round setup;
- strategy changes beyond the intended intervention;
- a strategy that is too unrealistic to serve as a meaningful baseline;
- metrics that accidentally condition on already being ahead.

The canonical control for design research is intended to be **Human Baseline**, not Mechanical Control. Mechanical Control remains valuable as a deterministic bottom benchmark and as a fallback for exact scripted integration tests.
