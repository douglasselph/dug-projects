# B06 — Sunlight regression coverage

The existing A05SunlightSourceOfTruthTest proves all three canonical CSV Sunlight effects grant one physical token, preserve die faces, and record no DieValueChanged events. B02SunlightRoundRuntimeTest covers ordinary and research-resync full-round paths.

The new B06SunlightSupplyRegressionTest covers each of the three cards when Grove supply has one token remaining (first player gains exactly one; another actor cannot execute) and when it starts empty (gain not executable). This protects global shared-supply conservation.

Test command:

    ./gradlew test --tests '*A05SunlightSourceOfTruthTest' --tests '*B06SunlightSupplyRegressionTest'
    ./gradlew simulationTest --tests '*B02SunlightRoundRuntimeTest'

No production code or official game data changed. These tests require execution under normal Ubuntu Gradle to be considered fully verified.
