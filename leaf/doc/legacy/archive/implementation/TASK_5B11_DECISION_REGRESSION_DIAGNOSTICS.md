# Task 5B.11 — decision-level diagnostics and regression map

Source baseline: `leaf(20261022-222505).tgz`. This package adds two test classes, with 11 deliberately contrasted scenarios, and retains all existing behavior. No production logic, effect execution, weight, or research data is changed.

## New regression positions

| Test | Choice/expected behavior | Layer |
|---|---|---|
| Petal P1/P2 against opponent discard | Discard theft excluded despite visible discard D4 | Legality |
| Petal P3/P4 against opponent discard | Discard theft included | Legality |
| All Petal variants without D4 sources | No fictitious legal acquisition | Legality |
| All Petal variants with three source classes | Activation score equals maximum full source+cycling score | Activation/target alignment |
| Sequential capped Raise on D6 | First Raise reaches 6; second yields no additional purchasing power | Projection outcome |
| Draw D6 after Raise | 3.5 expected pips and exact crossed 7/9 purchase tiers | Projection outcome |
| Top-row wound risk from 5-point gap | +1 improvement is positive, only top row changes | Tactical consequence |
| Sibling hypothetical Raise branches | Independent outcomes and original unchanged | Isolation |
| Grove/opponent Bee sources absent/present | Missing sources report null; present source has score | Legality and scoring |
| Bee theft with both sources available | Opponent denial makes stealing preferable | Target ranking |
| Ready versus spent opponent Butterfly | Ready denial more valuable | Target ranking |
| 0/1/2 spent own Butterflies | Refresh benefit increases with actual spent count | Resource consequence |
| Bee with/without acquisition source | Source availability changes complete effect valuation | Projection consequence |

Note: Some tests include multiple assertions/positions; number of JUnit test methods is 11.

## Existing decision/execution contracts retained

| Family | Existing tests to retain | Coverage and limitation |
|---|---|---|
| Sapping S1/S2 | `BattleSappingSnapdragonProjectorTest`, `SappingSnapdragonVariantsRegressionTest` | Legal ordered die pairs, two-row projection, caps, real execution; no end-to-end decision followed by executor in one test |
| Petal P1–P4 | `PetalAcquisitionProjectionTest`, `PetalToDie4EffectTest` | Acquisition source legality, source rankings, activation alignment, execution behavior; not a single end-to-end execution fixture |
| O Edelweiss | `HumanBaselineEffectStrategyOEdelweissBattleAlignmentTest`, `OEdelweissEffectTest`, `OEdelweissVariantsTest` | Two-choice facing and Flip→Play, nested planning; projected first-step resources not exhaustively verified against executor |
| Bloom/Gust/Vine Punishment | `BattleCollateralActivationPriorityTest`, `GustOfPetalsBattleTest` | Activation and target tactical values; multi-row Vine branch and first Draw of Transplant still need complete projections |
| Draw/replace/upgrade | `ExpectedDrawProjectionTest`, `HumanStateProjectionTest` | No RNG, expected-value arithmetic, supply/refill, capped Raises; Battle sequential placement remains approximate |
| Bee/Butterfly/Plant-facing | `HumanBaselineEffectStrategyResourceChoiceTest`, `HumanBaselineEffectStrategyPlantAlignmentTest`, `PlantResourceProjectionTest` | Observable resources, target choice and simple activation agreement; future value intentionally approximate |
| Buy/Main actions | `HumanBaselineBuyStrategyTest`, `HumanBaselineCultivationStrategyTest`, `PlantAcquisitionUsabilityTest` | Purchase legality and local priorities; global relative calibration and individual-die multiple-purchase optimization still open |
| Whole effect execution | `CardEffectContractTest` (integration), special effect tests | Execution legality and effect behavior independent of Human Baseline selection |

## Diagnostic failure interpretation

- **Legal option failure:** Check request-provided `legalChoices` and consistency of `DecisionContext`; never add phantom resource fixtures.
- **Activation-versus-target failure:** Compare exact shared branch scores *before* adding the action-level base score; watch for duplicated projected bonus.
- **Mean/threshold failure:** Keep `Double` through projection; never turn fractional predictions into physical rolled faces or trigger Roll Rewards.
- **Battle outcome failure:** Inspect affected rows separately; measure win/tie, +2 Strike VP, 5-point Wound threshold, and cap after *actual* reductions.
- **Executor consistency failure:** Verify a decision picked from a legal request is run against the same live-state fixture; projected state must never be fed back as a real engine state.

## Outstanding coverage before 5B.12

1. Build full choose-then-execute fixtures for Petal, Sapping, O Edelweiss, and Battle collateral, asserting the resulting game-state delta rather than just the presence of legal decisions or separate engine tests.
2. Add deliberately opposed Cultivation Main Action positions: Draw versus a Plant activation versus a Round effect at a reachable purchase threshold.
3. Add a Buy comparison with identical immediate resources but different remaining phase opportunity counts, and ensure no Buy-only usability premium leaks into Main Action.
4. Verify nested O Edelweiss Draw→Raise, Raise→Raise, and resource-dependent Play→Play against the **actual** executor in precisely constructed contexts.
5. Solve sequential Battle Draw placement capacity to avoid reusing the same empty pad for each expected die.

These are specifically labeled unresolved rather than reporting inferred execution consistency as verified. A dedicated end-to-end harness is the next material test infrastructure investment.

## Run

```bash
./gradlew test --tests '*HumanBaselineDecisionDiagnosticsTest' \
  --tests '*HumanBaselineResourceDecisionDiagnosticsTest'
./gradlew test integrationTest simulationTest
```

Local build attempt was started but did not finish; compile/test passage is not claimed.
