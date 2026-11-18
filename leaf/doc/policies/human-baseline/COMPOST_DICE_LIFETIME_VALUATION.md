# Normal Round Compost — future dice-lifetime valuation

**Implementation checkpoint: 18 November 2026.** The source has been updated for **ordinary** v38 Round Compost. Full project Gradle acceptance and real seeded gameplay comparisons have **not** been run in this environment. Do not mark this gate complete until the Ubuntu checks below pass. No official card, game-rule executor, learned/frozen policy or resource personality was changed.

## Production decisions changed

`CompostLifetimeValuation.compare(context, normalPurchasingPower)` is a pure, read-only projection of **each individual Hand die** against the *actual* next Draw. It reuses `FutureDiceForecast`, `FutureDiceZones` and `ImmediatePipValuation` and reads only the legal visible `DecisionContext` snapshot. It does **not** sample dice or strategy RNG.

* For each Hand die, determine whether the **next** normal die size is physically available. If it is, remove the current die from projected Hand and put its upgraded replacement in projected Discard. If the next size is unavailable, or the die is D20, **Trash that die** in the projection. Never skip an unavailable upgrade size.
* Charge the **actual current face value** for taking that die out of Hand this round. The upgraded die is **not usable now** under ordinary Round Compost.
* Forecast the rest of the dice collection through the publicly known future Cultivation/Battle phase pattern with the same default four Draws per future Cultivation (three/five alternatives are supported by the projection) and three Draws at Battle opening. Only future encounters of the replaced die/affected pool count. Record separately the expected **future spending pip** difference and the provisional **future Critter/Wisp equivalents** difference.
* Forecast a normal Draw from the actual lowest-sided Supply die, refilling from Discard only if Supply is empty. A current Draw provides the corresponding current mean roll and eligible reward option values; it also changes what will be available later. Never treat a newly purchased or upgraded die as instantly drawable.
* Retain one **separate, visible Buy threshold** adjustment when Compost would remove enough current purchasing power to lose a currently affordable tier. The provisional legacy-scale value remains `-10` points per lost tier. This is a judgment about same-round purchasing options, **not** additional dice-lifetime pips. Purchases, overpayment and future buying choices are not fully simulated.

The score used by `HumanBaselineCultivationStrategy` is anchored on the **current Draw priority scale** for compatibility with unmigrated Plant/other Round effects. Its action-specific difference from Draw is the signed, future-aware pip delta, at four temporary legacy points per pip, followed by the explicit lost-Buy-tier term. This is **not** a claim that all Build actions have already been migrated to common pip-equivalent scoring.

`CompostLifetimePriority.bestTarget` and `targetScore` share the **same** projection/scoring terms. The normal Round activation and its downstream `HumanBaselineEffectStrategy.chooseDie` therefore select the same forecast-optimal die (absent additional external policy influences or genuine tie decisions).

The old normal Compost probabilistic willingness gate (75%/50%/10%, etc.) is no longer invoked in the **default** production strategy. Otherwise even a plainly superior upgrade could be discarded randomly, defeating the specified comparison. Optional modern-scoring variability remains distinct from this pure forecast. Water/Sunlight/Mulch acquisition personalities remain **unchanged** and do **not** enter Compost mathematics, nor are they allowed to influence spending. The older `CompostPriority` class remains temporarily for legacy certification and the **distinct** Upgrade-and-use-now variations; it is not the production normal Round Compost evaluator.

## Known modeling boundaries

Future forecast does not anticipate unknown purchases, Wisp/Plant extra Draws, Battle Doom, subsequent upgrades, mutable resource stocks, or future decisions. It uses the same assumptions for all action alternatives. The expected-value comparison is therefore a human baseline heuristic, **not** a full probability distribution of purchase/win outcomes. The read-only `CommonBuildActionValuation` may still report a different pip-only target because it does not apply the separate Buy-tier penalty; this is not silently represented as aligned. A later common-valuation migration must reconcile all still-unpriced consequences.

## Tests and acceptance

Focused new tests: `CompostLifetimeValuationTest` (early/late, multi-round cycling, Supply delay, Discard refill, legal Trash, target parity, Buy-tier effects, personality independence). `HumanBaselineCultivationStrategyTest` now verifies normal Compost versus Draw on a state with a real future upgrade opportunity and verifies that changing the old percentage-gate randomizer does not change the choice. `HumanBaselineEffectStrategyTest` verifies that the actual downstream chooser picks D12→D20 over D4→D6 when both are legal and a future Battle will use the upgraded die.

Run from the repository root on Ubuntu:

```bash
./gradlew test \
  --tests '*ImmediatePipValuationTest' \
  --tests '*FutureDiceForecastTest' \
  --tests '*CommonBuildActionValuationTest' \
  --tests '*CompostLifetimeValuationTest' \
  --tests '*HumanBaselineCultivationStrategyTest' \
  --tests '*HumanBaselineEffectStrategyTest' \
  --tests '*CultivationMainPriorityTest' \
  --tests '*MulchCompostDecisionQualityTest' \
  --tests '*ResourceSpendingPersonalityIndependenceTest'
./gradlew test integrationTest simulationTest
```

A local standalone Kotlin 1.9 compilation/run using the **actual** `FutureDiceForecast`, `CompostLifetimeValuation`, and `CompostLifetimePriority` sources and lightweight surrounding-type stubs passed six forecast/choice smoke scenarios. That is **not** project compilation or a JUnit/Gradle acceptance pass. The Gradle wrapper required downloading 8.13 and could not resolve `services.gradle.org` in this environment. The Ubuntu output and a small A/B Build snapshot (both identical seeds and personality profiles) remain necessary before accepting behavior changes.
