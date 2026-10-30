# Task 5B.11 final execution regressions

Adds three regression tests to `HumanBaselineChooseThenExecuteTest`:

1. **Petal P1–P4 cycling**: an actual Human Baseline chooses a legal D4 discard subset after Grove acquisition. The real effect executor discards exactly that subset and Draws the same number of deterministic D8/D10 dice from Supply. Assertions cover Hand, Supply, Discard, and Graft Bed ownership, identity of drawn dice, and legal source choice. The position uses a D4 showing 1 to make optional cycling attractive.
2. **O Edelweiss Play → next decision**: a scripted *first* choice Plays a registered face-up Raise +1 Vine. The real nested executor Raises the Hand die and spends the Vine. The *second* choice is made by the unmodified Human Baseline and is checked against its live `DecisionContext`, including the changed die value and Vine facing.
3. **O Edelweiss Flip → next decision**: a scripted first Flip turns a registered spent Vine up. The actual executor rebuilds the second legal choices; the normal Human Baseline must observe the face-up Vine and the newly legal Play option.

The first Edelweiss move is deliberately scripted to ensure a known state-changing setup. The second is always selected by the real Human Baseline. These are execution/state-observation tests rather than assertions that the planner's *numeric prediction* is exactly equal to a realized stochastic result. The tests do not consume random numbers during prediction; mechanical Draws use fixed test dice to suppress roll variation.

Run `./gradlew test --tests '*HumanBaselineChooseThenExecuteTest'`, then `./gradlew test integrationTest simulationTest`.

**Validation status:** The local Gradle attempt did not finish compilation; tests are not claimed to pass.
