# C04 — Positive Sunlight-funded Worm continuation regression

Adds `execute_sunlightFundedWormFlipsTiedStrike_beforeOrdinaryFinalMain` to the existing Battle Action Coordinator integration harness.

The first and funded Main are fixed only to construct the scenario. The actual Human Baseline handles the newly generated Support and Final Main decisions. Both players start with a D4 showing 3 on the top Strike row. The funded Round action grants two Worms; the baseline is expected to place one on TOP, changing a tie to a win. Assert chronological funded Main → Worm Support → ordinary Final Main, and conservation of the spent Sunlight token.

This is a behavioral acceptance test. If it fails, inspect tactical Support gates and turn continuation rather than changing expected behavior to match a weak choice.

Verification:

    ./gradlew test --tests '*BattleActionCoordinatorTest'
    ./gradlew test simulationTest
    python3 bin/a03_verify_boundaries.py

The Kotlin test could not run in the packaging environment because the Gradle wrapper requires an unavailable remote distribution. A03 succeeded with no protected-file changes.
