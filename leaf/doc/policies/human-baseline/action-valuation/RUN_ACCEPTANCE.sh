#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
echo 'C04 ACCEPTANCE: focused Battle selection and funded Worm behavior'
./gradlew test --tests '*C04RealPlantMixedSelectionTest' \
  --tests '*C04MultiplayerMixedActionMatrixTest' \
  --tests '*C04ResourceUtilityParityTest' \
  --tests '*C04CollateralEndToEndTargetChoiceTest' \
  --tests '*C04MultiplayerRowTargetParityTest' \
  --tests '*BattleActionCoordinatorTest'
echo 'C04 ACCEPTANCE: full Kotlin and simulation regressions'
./gradlew test simulationTest
echo 'C04 ACCEPTANCE: strict research change boundaries'
python3 bin/a03_verify_boundaries.py
echo 'C04 ACCEPTANCE: regression pipeline PASS (human preference calibration remains unverified)'
