# B03 follow-up — fix the final R0 full-suite test failure

## Observed result

- `./gradlew test`: 1,617 tests, 1 failure.
- Remaining test: `R0CorrectnessAuditTest.fullSeededGameIsIdenticalWithAndWithoutBuyShadowAndDevelopmentTracking`.
- Exception at `R0CorrectnessAuditTest.kt:95` while loading the Plant CSV files from a Koin-supplied `PlantCardRegistry`.

## Reproduced root cause

`GameModuleWiringTest` creates two `koinApplication { modules(appModules) }` instances without closing them. The top-level `appModules` list contains shared Koin `Module` definitions, whose singleton factories can be reused across Koin applications. A registry populated by one test therefore remains in the singleton factory when another application is created.

Direct reproduction in the current source environment:

```
a: registry A, cards=12
b: same registry A, cards=12
b.loadFromCsv(...) -> IllegalArgumentException:
  Duplicate Plant card name 'Root_05_01'
```

Closing the application resets its singleton factory, so a fresh application obtains a fresh registry with zero previously loaded cards.

## Minimal fix

File: `src/test/kotlin/dugsolutions/leaf/v35/game/di/GameModuleWiringTest.kt`

1. Save each Koin application in an `app` variable.
2. Wrap every test's use of that application in `try`/`finally { app.close() }`.
3. Add `closedProductionApplicationReleasesSharedCatalogSingletons` to prove registry identity and card contents are reset after close.

No changes to `R0CorrectnessAuditTest`, production classes, Plant market legality, Sunlight mechanics, learned policies, research launchers, or data. Do not fix this by making `PlantCardRegistry.loadFromCsv` ignore duplicate names.

## Verification performed in this environment

- Direct Kotlin demonstration reproduced shared registry identity and duplicate-name failure.
- Compiled and executed four Kotlin test methods consecutively using real production classes and Koin runtime, with a minimal local `@Test` annotation stub and Kotlin's assertion library: **4/4 pass**.
- Python suite: **47/47 pass**.
- A03 boundary check with `doc/B03_PROPOSED_A03_PROTECTED_BASELINE.json`: **479/479 protected files unchanged**.
- Full Gradle `test`/`simulationTest` could not be executed here: the offline test dependency cache lacks several dependencies. Run them on Ubuntu.

## Ubuntu verification commands

```
./gradlew test --tests '*GameModuleWiringTest' --tests '*R0CorrectnessAuditTest'
./gradlew test simulationTest
```

## A03 manifest note

The current archive still contains the original pre-B03 `doc/A03_PROTECTED_BASELINE.json` and the reviewed post-B03 `doc/B03_PROPOSED_A03_PROTECTED_BASELINE.json`. Running `python3 bin/a03_verify_boundaries.py` against the original manifest reports B03's five already-documented mechanical changes. Use the approved B03 procedure or the reviewed proposed manifest for that check; this test-only patch introduces **no additional protected changes**.
