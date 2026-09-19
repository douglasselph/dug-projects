# Testing and Verification

The project has three verification layers that should normally be kept distinct: production unit tests, deterministic integration tests, and simulation-layer tests.

## 1. Standard commands

### Unit tests

```bash
./gradlew test
```

The current Gradle configuration filters the normal `test` task to v35 tests under:

```text
dugsolutions.leaf.v35.*
```

### Integration tests

```bash
./gradlew integrationTest
```

or the equivalent v35-named task:

```bash
./gradlew v35IntegrationTest
```

`integrationTest` depends on `test`, so an integration run first verifies the production unit suite.

### Simulation-layer tests

```bash
./gradlew simulationTest
```

### Simulation compile + tests

```bash
./gradlew simulationCheck
```

This verifies that the research source set compiles against production code and that its simulation tests pass.

### Full current verification pass

```bash
./gradlew test integrationTest simulationCheck
```

This is the most useful pre-commit sanity command when changes may affect the engine, deterministic scenarios, and research layer.

### Human-readable whole-game smoke run

```bash
./gradlew runMechanicalGameSmoke
```

This is intentionally different from a pass/fail test. It runs a complete real-engine game with Mechanical Control players and writes `summary.txt` and `chronicle.txt` beneath `output/smoke/mechanical-control/`. Use it when you want to inspect the actual sequence of play rather than only learn that assertions passed.

A different deterministic seed can be supplied with:

```bash
./gradlew runMechanicalGameSmoke -PsmokeSeed=24680
```

---

## 2. What each layer should prove

### Unit tests — local correctness

Unit tests should answer questions such as:

- Does this converter parse a card effect correctly?
- Does this scoring primitive combine adjustments correctly?
- Does this die helper calculate the expected value correctly?
- Does this effect handler mutate the intended domain object correctly?

They should usually be fast and narrow.

### Integration tests — real-engine correctness

Integration tests answer:

> Do the real components work together correctly when the entire scenario is controlled?

The integration harness uses production CSV loading, the production object graph, real round coordinators, real effects, real game state, and typed Chronicle events. Test-only controls provide exact decisions and randomness.

Integration tests should favor exact state assertions and invariants, not statistics.

### Simulation tests — research tooling correctness

`src/simulationTest` tests the simulation machinery itself:

- experiment configuration validation;
- seat/game-count math;
- strategy modifiers;
- result aggregation;
- report formatting;
- plan semantics.

A simulation-layer unit test should not need to run 10,000 games to prove that an aggregator computes the right formula.

---

## 3. Browsing the current test suites

### Production tests

```text
src/test/kotlin/dugsolutions/leaf/v35/
```

Browse by package corresponding to the production subsystem you are changing.

### Integration tests

```text
src/integration/kotlin/dugsolutions/leaf/integration/v35/
```

Current organization:

```text
chronicle/
sanity/
  battle/
  cultivation/
  decision/
  effect/
  game/
  round/
  scoring/
  setup/
support/
  decision/
  random/
```

The `sanity` name is deliberate: these tests verify that the complete engine path has coherent behavior, without trying to duplicate every unit-test edge case.

### Simulation tests

```text
src/simulationTest/kotlin/dugsolutions/leaf/simulation/v35/
```

The current suite includes tests for planned strategies and card-experiment configuration, aggregation, and reporting.

---

## 4. Determinism and reproducibility

There are two distinct random streams.

### Mechanical RNG

`GameConfig.seed`

Controls game mechanics such as dice, deck shuffles, and random game effects.

### Strategy tie RNG

`GameConfig.strategySeed`

Controls randomness used only when a strategy needs to break equal-scoring choices.

By default the strategy seed follows the mechanical seed, so a game can be reproduced with one seed value. For experiments, they can be supplied independently.

This separation is essential when comparing two strategies: changing how a bot resolves a score tie should not silently advance the die/deck RNG and thereby change the physical game it experiences.

For exact integration tests, prefer `ScriptedRandomizer` when a specific next result matters. A fixed seed is deterministic, but it is brittle if unrelated random calls are later added earlier in the execution path.

---

## 5. Chronicle as a verification surface

The production Chronicle uses typed events. Integration tests can therefore verify events such as:

- Round reveal/completion;
- die rolls and Roll Rewards;
- Main and Support Actions;
- effects;
- purchases and grafts;
- Battle order and Strikes;
- Wounds and Doom;
- Refresh/Cleanup;
- final scoring/game completion;
- optional decision reasoning when enabled.

Prefer querying typed Chronicle entries over asserting formatted report strings. Presentation text can change without changing game semantics.

The integration support package contains reusable `ChronicleQueries` and `ChronicleAssertions` for this purpose.

For human review, `ChronicleTextRenderer` converts the same typed entries into a line-oriented diagnostic report. The smoke runner persists that rendering under `output/`; tests should continue to assert typed entries rather than formatted text.

---

## 6. Snapshot checks

Integration snapshots provide an immutable view of important state after execution.

Useful snapshot types include:

- `GameSnapshot`
- `PlayerSnapshot`
- `GroveSnapshot`
- `BattleSnapshot`

They make tests easier to read because assertions can describe the post-condition directly instead of reaching into many mutable production objects.

A good integration test often asserts both:

1. **state** — what the game looks like after execution; and
2. **Chronicle** — what significant sequence of events occurred to get there.

---

## 7. Adding a regression test after a bug

When a simulation or playtest exposes a rules-engine defect:

1. Reduce the problem to the smallest deterministic reproduction.
2. Add a unit test if one class owns the bug.
3. Add or extend an integration scenario if the bug depends on subsystem interaction.
4. Fix production code.
5. Run the full relevant verification set.
6. Only then rerun the statistical experiment whose result may have been affected.

Do not “fix” a suspicious simulation result by modifying the simulation layer until engine correctness has been checked.

---

## 8. Suggested pre-commit checks

For documentation-only changes:

```bash
git diff --check
```

For production-rule changes:

```bash
./gradlew test integrationTest
```

For decision/strategy or experiment changes:

```bash
./gradlew test integrationTest simulationCheck
```

For changes to a specific experiment, also run that experiment at a small sample size first to catch configuration problems before launching a large batch.
