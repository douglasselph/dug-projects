# C04 coordinator-legal acceptance — incremental boundary

This patch adds two tests to `BattleActionCoordinatorTest` that instantiate actual `Game`, `BattleState`, Player dice (three initial hand dice each), `BattleActionCoordinator`, and coordinator-generated first Main choices.

- `c04_reviewedV3_18_realGameLegalDrawVersusRoundVp_reportsActualRanking`: uses an **adapted two-player** decision modeled on V3-18's VP-vs-Draw question. The coordinator must offer both actions; `BattleMainPriority` evaluates those exact actions against the first Main observation. Prints a `C04_ENGINE_LEGAL` diagnostic with scores and author's **original** B/confidence=4 preference as reference only. **Not** a faithful four-player V3-18 reproduction, and `agree=true` would NOT certify agreement with the author's exact original position.
- `c04_reviewedLegalChoice_absentDrawIsExcludedByRealCoordinator`: proves no Draw choice is offered when no die can cycle even if a hypothetical questionnaire says it could. This validates the real legal-choice boundary.

These are coordinator **legal candidate** integration fixtures; they do NOT yet exercise exact-target Plant decisions through the production effect-target selector. Do not freeze any Plant preference from the questionnaire based on these cases. The round effect execution is a test `RecordingEffects` stub; it validates opportunity and scoring, not real reward resolution.

## Run on Ubuntu

```sh
./gradlew test --tests '*BattleActionCoordinatorTest' --info | tee c04-coordinator-acceptance.log
./gradlew test simulationTest
python3 bin/a03_verify_boundaries.py
```

Search stdout/log for `C04_ENGINE_LEGAL|`. Gradle may redirect test output into `build/test-results/test/TEST-*.xml`; if absent, rerun with `--info`.

**User action:** You only need to install and run tests. There is NO questionnaire or manual review requirement. If a test fails, or you would like me to analyze the measured scores, paste the `C04_ENGINE_LEGAL` line(s) or the failing test stack trace. This is the data needed before adjusting coefficients.

## Verification limits

Local Gradle did not reach compilation/tests before its timeout. The A03 protected-boundary check passed (480/480). More cases and faithful exact-target integration remain future work; C04 remains unchecked.
