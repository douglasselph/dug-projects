# C04 multiplayer Draw/Wisp regression correction

The previous matrix asserted that Battle Draw has identical value with and
without available Wisps. This became false when the C04 Draw audit correctly
added the chance of gaining a Wisp on a roll of 2 during Battle.

The test now verifies: (1) an exhausted Round Wisp action has zero value,
(2) an available Wisp has value, (3) Draw itself values the roll-2 Wisp
opportunity, and (4) Draw remains more useful than an exhausted Wisp action.
No production code, coefficients, or canonical rules changed.

Run:

    ./gradlew test --tests '*C04MultiplayerMixedActionMatrixTest'
    ./gradlew test simulationTest
    python3 bin/a03_verify_boundaries.py
