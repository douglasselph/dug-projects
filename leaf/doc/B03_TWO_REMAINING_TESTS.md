# B03 follow-up: remaining Gradle test failures

## Observed on Ubuntu

After the first fixture repair, `./gradlew test` completed 1,617 tests with two failures:

- `BuyCoordinatorTest.execute_filtersIllegalUnavailableUnaffordableAndD4Items`: `NoSuchElementException`, line 252.
- `R0CorrectnessAuditTest.fullSeededGameIsIdenticalWithAndWithoutBuyShadowAndDevelopmentTracking`: `IllegalArgumentException`, line 95.

## BuyCoordinatorTest — fixed

The A02-compliant fixture has exactly one Root-cost-5 Grove slot. The test exhausts this stack, and separately exhausts all nine D6 dice. A buyer with six pips is then unable to purchase **anything**: no Buy request is forwarded to the strategy, so `.purchaseRequests.single()` fails. This is expected coordinator behavior.

Change the test buyer's die from 6 to 7. This preserves the exhausted Root-5 and D6 conditions while leaving an affordable Root-7 card in the legal Grove. Assert that a cost-7 Plant was offered; update all affordability expectations from 6 to 7.

This modifies *only the test setup and assertions*, not the Buy coordinator or physical inventory.

### Verification

The patched Buy test was compiled with Kotlin CLI against the latest locally compiled production classes and executed in a direct test harness: PASS.

## R0CorrectnessAuditTest — diagnosis still required

The direct Kotlin CLI harness independently compiled and executed the **unmodified** `fullSeededGameIsIdenticalWithAndWithoutBuyShadowAndDevelopmentTracking()` from the same source snapshot: PASS. Its `PlantCardRegistry.loadFromCsv(...)` at line 95 succeeded and it ran the entire seeded comparison successfully. Thus the sole line-number summary is insufficient to infer why the Ubuntu full suite reported `IllegalArgumentException`.

Do not change the production CSV parser, Grove validator, or R0 assertion to hide a potentially order-dependent failure.

Run just the focused R0 test on Ubuntu:

```bash
./gradlew test --tests '*R0CorrectnessAuditTest'
```

If it fails, send `build/test-results/test/TEST-dugsolutions.leaf.v35.research.R0CorrectnessAuditTest.xml` so the actual exception **message and cause stack trace** can be diagnosed. If it passes alone but fails in `./gradlew test`, the next investigation is test interference/order dependence; run the suite with `--no-parallel --max-workers=1` and preserve the failing XML. A generic `--stacktrace` on Gradle mostly shows Gradle task failure, not the individual JUnit cause; the XML is the useful artifact.

### Suggested validation

```bash
./gradlew test \
  --tests '*BuyCoordinatorTest' \
  --tests '*R0CorrectnessAuditTest'

./gradlew test simulationTest
```

This follow-up has made no production, canonical rules, market data, policy, or learned weight changes. The A03 check with `doc/B03_VERIFY_A03_BOUNDARIES.sh` passed, recognizing B03's previously documented five protected changes.
