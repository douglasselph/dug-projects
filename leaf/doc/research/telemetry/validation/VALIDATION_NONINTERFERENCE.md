# D8 — Validation and noninterference checkpoint

## Scope

D8 adds opt-in regression tests without changing the game engine, Human Baseline, learned expert, or telemetry schemas. This is **not** D9 production-runner integration.

### New focused tests

- `BuyDiagnosticNoninterferenceTest`: trace-equivalence of wrapped and unwrapped Buy decisions (including payment), actual strategy random draw count equivalence, event/comparison sequence consistency, and no hypothetical payment execution on divergence. Repeated Buy phases and purchase positions are exercised with seeded random strategies.
- `DevelopmentNoninterferenceTest`: compare graft/Snip/die ownership transition traces with and without `PlayerDevelopmentTracker`, and verify snapshots are historical copies.

### Existing regression suites to rerun

- `BuyDisagreementTest`
- `ShadowBuyStrategyTest`
- `BuyDecisionEventTest`
- `BuyDisagreementReportsTest`
- `PlayerDevelopmentTrackerTest`
- `PlayerDevelopmentReportsTest`

### Run from the repository root

```bash
./gradlew test \
  --tests '*BuyDiagnosticNoninterferenceTest' \
  --tests '*DevelopmentNoninterferenceTest' \
  --tests '*BuyDisagreementTest' \
  --tests '*ShadowBuyStrategyTest' \
  --tests '*BuyDecisionEventTest' \
  --tests '*BuyDisagreementReportsTest' \
  --tests '*PlayerDevelopmentTrackerTest' \
  --tests '*PlayerDevelopmentReportsTest' \
  |& tee ~/Downloads/leaf-D8-tests.log
```

Then run the full test suite with `./gradlew test`.

### Confidence boundaries

These tests directly check the wrapper/observer behavior, but **do not yet prove bit-for-bit parity of complete seeded simulation games**, because D9 has not connected diagnostics to production evaluations. A paired full-game test comparing seed, results, score, battle outcomes and mechanical RNG should be an explicit D9/D10 acceptance gate. The tests also do not demonstrate an independently played Human Baseline outcome: the shadow only recommends purchases from the active player's observed state.

## Local validation status

The bundled offline Gradle test launch reached `:compileKotlin` but did not complete within the execution window. Do not assume compilation or test success until the above commands return zero exit status.
