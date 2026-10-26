# C04 follow-up: Battle utility and Sunlight preservation regression

Input: leaf(20261026-170111).tgz plus C04 Battle resource utility calibration patch.

## Why changing only assertions is insufficient

Sunlight preservation previously used 55 baseline / 62 early / 52 with surplus, originating in the legacy 50–100 priority convention. After Battle Draw migrated to 35 + rounded expected face + tactical projection and future resources migrated to discounted values, a D20 Draw with no Strike context scores 46 and a future D20-to-Discard at two future Battle opportunities scores 43. The legacy Sunlight threshold would suppress both actions even in situations meant to demonstrate worthwhile or surplus-token spending.

The preservation policy remains explicitly provisional and shallow, but its thresholds are placed in the same Battle decision-point band: baseline 39, early +5, extra held token -4, final -24 (floor 5). Values: last token early 44; two tokens early 40; last token mid 39; final 15. These are *not* learned or empirically calibrated weights and should be validated by simulation and human decisions later. Game rules are unchanged.

## Regression test updates

- Battle Draw D4: 38; D20: 46 (no Strike context), not obsolete Cultivation x4 values 45/77.
- Gain available Wisp: 40, not obsolete 50.
- Steal three opponent Wisps: 56 (35+3*(5 acquisition+2 denial)), not obsolete 71.
- Gain D20 to Discard at >=2 reported future Battles: 43 (35+round(10.5*0.75)), not obsolete 56.
- No future Battles: 0. The older test must explicitly supply battleRoundsRemaining instead of total remaining Round-card reveals.
- Main-stage projection is *already included* in intrinsic Draw; assertion must compare equality, not expect a second projection.
- One immediate non-Strike VP: 38, tying a D4 Draw in the bare fixture; no arbitrary tie-break ranking is asserted.
- Refresh and Worms have unprojected contextual benefit; test checks the actual explanation, not one stale phrase.

## Verification

Run on Ubuntu:

    ./gradlew test --tests '*SunlightBattleSpendingValueTest' --tests '*BattleTurnOrchestratorTest' --tests '*C04MainStageParityTest' --tests '*C04MixedCandidateSelectionTest' --tests '*C04RoundEffectContextTest' --tests '*C04RoundScaleMixedSelectionTest' --tests '*C04ResourceUtilityParityTest'
    ./gradlew test simulationTest
    python3 bin/a03_verify_boundaries.py

Local source boundary check passed with no changes. Kotlin tests were NOT executed locally: offline Gradle distribution was available, but Kotlin JVM Gradle plugin 1.9.21 was missing from its offline plugin cache.

C04 remains open pending actual suite execution and broader tactical valuation calibration. Do not treat numeric test updates alone as proof of valid decision rankings.
