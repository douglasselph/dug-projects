# C04 mixed-candidate selection regression pass

This test exercises the real BattleMainPriority.score entry point for Draw versus
Round candidates. It verifies that a scarce Wisp changes ranking, unavailable
opponent Wisp theft cannot beat a legal Draw, and the same D20 Draw gains
incremental Strike consequence when an actual contested row is supplied. It
also verifies parity with the First Main scorer.

These are **selection boundary tests**, not evidence that all Battle candidates
are calibrated. Fixed 35/45/50/60 priority offsets remain active, so C04 remains
unchecked until Plant, Round, compound and opponent actions all use calibrated
comparable final preferences. No gameplay or production code is modified.

Run:

    ./gradlew test --tests '*C04MixedCandidateSelectionTest'
    ./gradlew test simulationTest
    python3 bin/a03_verify_boundaries.py
