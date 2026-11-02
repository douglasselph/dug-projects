# B03 — Retire obsolete Sunlight Raise +3 (2026-10-25)

## Rule and scope

The ONLY canonical Sunlight Round effect is **GAIN_SUNLIGHT_TOKEN**. The old printed wording `🎲 Raise a die +3` is INVALID; it must produce a contextual input error, **not** an alias, auto-migration, or token effect. The Sunlight token remains usable to fund Battle Main actions; this patch does not modify those rules.

### Active-code changes

- Keep `GameEffect.RAISE_DIE_PLUS_3` as a **retired, non-executable enum tombstone** so historical chronicle strings and frozen policy feature names can still be read without reordering the enum. No card CSV or research override should use it.
- `GameEffectConverter` detects the retired printed phrase/name and throws a descriptive error including the source card name. Unknown wording remains UNKNOWN and the Round registry rejects it.
- `RoundExperimentConfigLoader` rejects the retired enum from research override files, reporting the filename, card and row. Deliberate modern overrides like `GAIN_SUNLIGHT_TOKEN` continue to load.
- `DefaultGameEffectExecutor.supports` and `.canExecute` now reject the retired effect; forced `.execute` fails before state mutation with an explicit message. The die-value handler no longer executes Raise +3.
- Human Baseline Cultivation validates that Round effects are not retired even when an invalid Round card is constructed programmatically; removes old 50% Sunlight Raise willingness gate and obsolete priorities. Official Gain Sunlight token still uses `SunlightTokenPriority`.
- Removes Raise +3 targeting and scoring from Plant strategies and Battle effect analysis; does not change normal +4 or research +20 mechanics.
- Retires unused +3 `SunlightPriority.kt` class completely; cleanup must remove the file on the user's filesystem, since ZIP extraction cannot delete an existing path.
- Tests which *previously endorsed* old Raise +3 were removed or migrated to valid Raise +4 mechanics. Historical chronicles keep references to the old effect as **data only**.

## Compatibility and intentional limits

- Historical `.weights`, experiment archives, and old chronicles are NOT rewritten. The old effect label survives only to decode historical metadata. Historic legacy experiment scripts may now fail instead of producing a misleading 'canonical Raise +3' run. That is deliberate. Update those experiments through an explicit, separate migration if needed.
- B03 is NOT the planned pip-scale recalibration, Battle unified valuation, or B04/B05 longer-term calibration study.
- B03 is explicitly approved to change the five A03-protected production paths and two simulation-source paths below; `A03` guardrail exceptions must list them individually. Tests/docs/decision-baseline files are allowed by A03.

## Apply to top-level leaf checkout

```bash
unzip -o ~/Downloads/leaf-B03-retire-obsolete-sunlight-raise3-changed-files.zip
bash doc/B03_REMOVE_OBSOLETE_SUNLIGHT.sh
bash doc/B03_VERIFY_A03_BOUNDARIES.sh
./gradlew test --tests '*B03RetiredSunlightRejectionTest' --tests '*DefaultGameEffectExecutorTest' --tests '*GameEffectConverterTest'
./gradlew simulationTest --tests '*RoundExperimentConfigLoaderTest' --tests '*B02SunlightRoundRuntimeTest'
python3 -m unittest discover -s bin/tests -p 'test_*.py' -v
```

These tests are distinct from full unit/integration/simulation regression; run `./gradlew test integrationTest simulationTest` when dependencies and environment permit.

## A03 explicit exception list

- `src/main/kotlin/dugsolutions/leaf/v35/effect/DefaultGameEffectExecutor.kt` — explicit B03 retirement of the obsolete effect.
- `src/main/kotlin/dugsolutions/leaf/v35/effect/GameEffect.kt` — explicit B03 retirement of the obsolete effect.
- `src/main/kotlin/dugsolutions/leaf/v35/effect/GameEffectConverter.kt` — explicit B03 retirement of the obsolete effect.
- `src/main/kotlin/dugsolutions/leaf/v35/effect/GameEffectDecisionRequirements.kt` — explicit B03 retirement of the obsolete effect.
- `src/main/kotlin/dugsolutions/leaf/v35/effect/handler/DieValueEffectHandler.kt` — explicit B03 retirement of the obsolete effect.
- `src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/round/RoundExperimentConfigLoader.kt` — explicit B03 retirement (simulation source is outside A03 fingerprint scope).
- `src/simulation/kotlin/dugsolutions/leaf/simulation/v35/learning/buy/EvaluateBuyPolicyMain.kt` — explicit B03 retirement (simulation source is outside A03 fingerprint scope).
- `src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/resource/SunlightPriority.kt` — within the permitted Human Baseline scoring package; file is intentionally deleted.

## Reviewed baseline acceptance (optional, after inspecting patch)

The archive contains `doc/B03_PROPOSED_A03_PROTECTED_BASELINE.json`, generated with the B03 five-file exception list. The checked-in `doc/A03_PROTECTED_BASELINE.json` is deliberately NOT silently overwritten.

Once you have inspected/accepted the five protected Kotlin changes, optionally run:

```bash
cp doc/B03_PROPOSED_A03_PROTECTED_BASELINE.json doc/A03_PROTECTED_BASELINE.json
python3 bin/a03_verify_boundaries.py
```

Commit the resulting manifest update as part of your reviewed B03 commit. Without accepting it, run `bash doc/B03_VERIFY_A03_BOUNDARIES.sh` to see the reviewed exception list; an unqualified A03 call will correctly continue to reject those five changes.

## Verification in preparation environment

- `./gradlew --offline compileKotlin compileSimulationKotlin`: **passed** (Gradle 8.13, Java 21 runtime, Kotlin target unchanged); all existing main/simulation sources compile.
- `python3 -m unittest discover -s bin/tests -p 'test_*.py' -q`: **47/47 passed**.
- Direct Kotlin runtime harness: official three Sunlight cards load as token effects; four legacy phrase forms reject; retired enum unsupported; current +4 and token supported — **passed**.
- Direct Kotlin harness executing `B03RetiredSunlightRejectionTest`: **3/3 passed**.
- Direct Kotlin research-loader smoke: Raise +3 enum override fails with file/row message; current token override succeeds — **passed**.
- A03 checker with B03's five explicit protected-path exceptions: **passed**.
- Full Gradle `test` and `simulationTest` tasks **not executed** here; run on Ubuntu before declaring all regression suites green.

## Test migration

Old fixtures that used Raise +3 as a neutral placeholder now use the supported Raise +4 where suitable. Historical expectation tests for the retired 50% chance / +3 threshold are removed; they no longer describe the current token rules. Historical *chronicle rendering* tests still retain old enum strings to validate reading old logs. The old `SunlightPriority.kt` scorer must be deleted using `doc/B03_REMOVE_OBSOLETE_SUNLIGHT.sh` after extraction.
