# Task 5B.11 follow-up: choose-then-execute checks

This package adds `HumanBaselineChooseThenExecuteTest` to the ordinary JUnit suite.

## End-to-end test contracts

- **Sapping S1:** invoke the real `DefaultGameEffectExecutor` with a live Battle grid and the `HumanBaselineEffectStrategy`; capture the die it chooses; independently calculate capped +2, the actual loss of opposing die pips (floor 1), and the resulting bonus to the selected die; check both actor/opponent dice and unchanged row placement.
- **Sapping S2:** same with two selected *different* dice, applying reductions to the chosen anchor die's row and its bonus to that die, not the initially raised die. Verify both rows independently.
- **Petal P1–P4:** make the Grove the only D4 source, let the Human Baseline choose it via the actual effect callback, then execute; assert Hand-versus-Discard destination, Grove supply consumption and unchanged D4 identity. Optional cycling is explicitly declined in this fixture to isolate acquisition and avoid random future draws.
- **O Edelweiss:** real effect invocation with a spent Raise Plant and a face-up O Edelweiss source; capture each of the Human Baseline's legal nested choices (up to two), assert legality and observable object invariants. This is an execution smoke test; it does NOT establish exact numeric agreement for nested-resource projections.

These tests reuse production `DecisionContextFactory` implicitly via effect handlers, rather than feeding custom synthetic contexts into the strategy. Fixed dice avoid RNG for the deterministic branches.

## Still needed before a strict end-to-end projection certification

1. Add live-state expected-versus-actual projection comparisons for Petal when it steals from an opponent, performs optional cycle-and-draw, and acquires during Battle (including relocation).
2. For O Edelweiss, assert a deliberately induced Flip→Play sequence and compare projected intermediate resources/Hand state with actual execution; also test Play→Play and nested Draw→Raise with controlled dice supply.
3. Compare the baseline's **top-level Main Action** selection against Draw, Plant and Round action competitors and then execute the selected action through the coordinator.
4. When predictions contain fractional means, compare against expected distribution calculations across controlled outcome fixtures, NOT equality to any single integer die roll.

## Validation

Test compilation/passing is not established in this environment. An offline Gradle attempt failed to resolve Kotlin plugin using an empty Gradle user home. Retrying with the bundled portable cache did not produce a Gradle task result within the execution window. Run on developer Ubuntu:

```
./gradlew test --tests '*HumanBaselineChooseThenExecuteTest'
./gradlew test integrationTest simulationTest
```

No game execution logic, card definitions or scorer weights are modified by this package.
