# C04 — provisional Battle resource utility calibration

This migration is intentionally **not a C04 sign-off**. Battle Draw contributes one point per expected pip; a die gained to Discard is now discounted to 50% of that direct benefit with one future Battle or 75% with two or more. It has zero current Strike-row value. A Wisp gains five provisional option points; stealing one adds two denial points. Direct non-Strike VP is three points. Zero available resource gains score zero. Complete Strike projections are unchanged and Strike VP is not credited here.

**Evidence limitation:** The 50%/75% future access and five/two Wisp figures are documented *assumptions*, not an empirically fitted calibration. Neither future access nor Wisp quality is actually predicted from the deck and action space. Refresh and Worm Round effects remain unprojected, with zero incremental points at a 35-point shared origin; Plant contextual fallbacks also remain uncalibrated. Do not check off C04 yet.

**Expected test migration:** Historical Sunlight and mixed-action tests assert values 50, 56, 71 and 77 based on earlier scoring. These need behavioral review (including whether Sunlight is economically justified), not rote numeric replacements. The existing C04MixedCandidateSelectionTest also expects Draw D4=45 even though the preceding Battle Draw migration gives 38. A green focused utility test does not imply a green full suite.

Verification: `./gradlew test --tests '*C04ResourceUtilityParityTest'` then `./gradlew test simulationTest` and `python3 bin/a03_verify_boundaries.py`. Changes touch only baseline Kotlin and tests/docs.
