# B03 follow-up: restore compatibility of Kotlin tests with A02 legal Grove invariants

## Symptom

Ubuntu `./gradlew test simulationTest` compiled sources, then `:test` reported
353 failures out of 1,616 tests. Many `IllegalArgumentException` failures occurred
in unrelated effect, Battle, coordination and game tests during test setup.

## Causes and fixes

1. **Shared test game fixture:** `GameEngineTestFixture.selectedCards()` generated
   three Roots, three Vines and three Flowers but assigned cost 5 to every card.
   Since A02, `PlantMarket` correctly requires exactly one of the nine official
   (type, cost) slots. The fixture now keeps its synthetic card IDs/effects and
   quantities but supplies the legal costs: Roots 5/7/9, Vines 7/9/11,
   Flowers 11/14/17.
2. **WoundResolverTest fixture:** it independently constructed nine cost-5 Plants.
   It now uses the same nine legal cost/type slots.
3. **Executor routing test:** B03 deliberately retained the enum value
   `RAISE_DIE_PLUS_3` for historical deserialization while disabling its
   executor route. The old routing assertion expected *every* non-UNKNOWN
   enum constant to have a handler. It now excludes only the explicitly retired
   constant, while a new test asserts that the retired effect has no handler.

## Boundaries

This patch changes only `src/test/` and this document. No production validation
is relaxed; no canonical effect, round, CSV, policy, weight, or mechanics changes.
The deprecation warnings from B02's legacy-reference tests are expected and
unrelated to the setup failures.

## Ubuntu verification

```bash
./gradlew test --tests '*DoomResolverTest' \
  --tests '*WoundResolverTest' \
  --tests '*DefaultGameEffectExecutorRoutingTest'

./gradlew test
./gradlew simulationTest
python3 bin/a03_verify_boundaries.py
```

Gradle generated XML reports under `build/test-results/test/` contain the full
exception text and stack traces. If any failures remain, inspect the **first
`<failure>` message**, not merely the console `IllegalArgumentException`
summary. Report remaining failures separately instead of attributing all of
them to A02.
