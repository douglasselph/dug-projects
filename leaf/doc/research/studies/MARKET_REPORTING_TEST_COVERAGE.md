# Leaf & Let Die — Current Market Reporting Test Coverage

Date: 2026-10-08

## Scope

This is a focused re-audit of the **current Plant-market analysis pipeline after the Bash-to-Python migration**. It covers only metrics used by the current-market confirmation/sanity reports and market-balance decisions.

The audited path is:

```text
game state / typed Chronicle event
  -> Kotlin market telemetry
  -> leaf.market-evaluation JSON
  -> Python MarketRawResult domain model
  -> Python aggregation
  -> final TSV reports
```

Human-readable evaluator console output is **not** part of the research-data path. It remains useful for diagnostics only.

### Reporting rule

**Human-readable evaluator output is for people. Research reports must
consume structured machine-readable telemetry.**

For current-market analysis, adding a parser for evaluator prose, indentation,
headers, or diagnostic text is considered an architectural regression.

## Current authoritative pipeline

### Kotlin boundary

The evaluator's typed market boundary is in:

```text
src/simulation/kotlin/dugsolutions/leaf/simulation/v35/learning/buy/
  EvaluateBuyPolicyMain.kt
  MarketEvaluationTelemetry.kt
  MarketEvaluationJson.kt
```

Important authoritative sources:

- **Grove exposure**: `MarketEvaluationTelemetryAccumulator.recordGame(resolvedGrove, winShare)` receives the actual resolved Grove for the affected role and increments each distinct Plant identity once per sample.
- **Plant purchase count**: `EvalAccumulator.add()` consumes typed `GameEntry.Purchase` entries for the affected player and calls `recordPlantPurchase(itemName)` for `PurchaseKind.PLANT`.
- **Plant type/cost**: the telemetry catalog is materialized from the effective `PlantValueResolver`, so research slot moves are represented by current effective metadata rather than inferred from the historical numeric tier embedded in the card ID.
- **Player count**: `MarketEvaluationTelemetry.playerCount`, populated from evaluator options.
- **Control / learned win share**: `MarketEvaluationTelemetry.overallWinShare`, accumulated from typed `PlayerGameSummary.winShare` for the affected seat.
- **Learner identity**: evaluator metadata carries `policyPath`; this is the stable identity passed to Python. Reports display its filename stem.

`MarketEvaluationJsonWriter` serializes those typed values directly under schema:

```text
leaf.market-evaluation / schemaVersion 1
```

### Python boundary

The current Python package is:

```text
src/research/python/leaf_experiments/market/
  models.py
  reader.py
  aggregation.py
  reports.py
  confirmation.py
  sanity.py
```

The data flow is:

```text
load_market_result()
  -> strict JSON/schema/invariant validation
  -> MarketRawResult / CardResult / LearnerResult
  -> aggregate_market_results()
  -> CardAggregate / SlotAggregate
  -> deterministic report renderers
```

Primary decision reports:

```text
card-purchases.tsv
card-summary.tsv
slot-summary.tsv
zero-purchase-cards.tsv
one-learner-only-cards.tsv
vine-9-summary.tsv
learner-win-shares.tsv
```

## Coverage matrix

| Metric | Authoritative source and trace | Kotlin tests | Python tests | End-to-end coverage | Invariants | Remaining risk |
|---|---|---|---|---|---|---|
| **Grove exposure** | Actual resolved `List<PlantCard>` passed by evaluator -> `MarketEvaluationTelemetryAccumulator.recordGame()` -> `groveExposureCount` -> JSON `controlExposure` / `learnedExposure` -> `CardResult.*_exposure` -> learned exposures summed into `CardAggregate.total_exposure` -> `grove_exposures` / `total_grove_exposures` / slot totals | `MarketEvaluationTelemetryTest.records exact per-card Grove exposure...`; duplicate identity counts once; `EvalAccumulator Plant purchase total...` now also asserts the full `EvalAccumulator.add()` path records Grove exposure | reader preserves exposure; aggregation exact arithmetic, zero-exposure behavior, multi-learner sums; report exact/NA tests | `test_market_pipeline_end_to_end_from_kotlin_json_to_final_reports` checks known 3/2/1 exposures through final TSV | non-negative; positive purchase cannot have zero exposure; complete 36-card market; 4 cards per legal slot; slot and market exposure reconciliation | Cross-language E2E begins at the Kotlin JSON boundary rather than launching a Kotlin simulation. The event/state -> telemetry side is covered separately in Kotlin tests. |
| **Purchase count** | Typed `GameEntry.Purchase` for affected player -> `EvalAccumulator.add()` filters `PurchaseKind.PLANT` -> `recordPlantPurchase(itemName)` -> `purchaseCount` -> JSON `controlPurchases` / `learnedPurchases` -> `CardResult` -> summed into card/slot reports | `records exact per-card purchases including multiple copies`; `sum of per-card purchases...`; `EvalAccumulator...reconciles...`; lower-level Buy tests verify typed purchase Chronicle events | reader purchase reconciliation; aggregation multiple purchases/exposure; report purchase reconciliation | E2E fixture checks nonzero purchases survive to raw and summary TSV; indentation regression explicitly proves console formatting cannot zero purchases | non-negative; per-card sums must equal role `plantPurchases`; positive purchase requires positive exposure | No remaining console-parsing dependency. Residual risk is limited to bugs in the actual Chronicle purchase event generation, which is separately covered by Buy mechanics tests. |
| **Purchases / exposure** | Not stored in Kotlin. Derived only in Python from learned raw `purchases / exposure` -> `safe_rate()` -> card/slot aggregate -> formatted report | Raw numerator/denominator tested separately; Kotlin intentionally does not derive this metric | `safe_rate`; zero denominator -> `None`; zero purchases with exposure -> real `0`; multiple purchases per exposure; precision tests | E2E checks `2/3 = 0.666667`, aggregate `3/7 = 0.428571`, `2/1 = 2.000000` | zero denominator is `NA`, never fabricated zero; raw counts retained alongside derived rate | This is an opportunity-frequency metric, not a probability: values greater than 1 are valid when multiple copies can be bought in one exposure. |
| **Plant type / cost** | Effective `PlantValueResolver.typeFor/costFor` when telemetry catalog is created -> JSON `type` / `cost` -> `CardResult` -> slot identity -> card and slot reports | `typed result preserves effective Plant type and cost metadata`; JSON exact serialization | reader legal-slot validation; current legal slot-move regression; aggregation rejects metadata disagreement between learners; report legal slot-move regression | E2E final rows assert type/cost | only legal slots; exactly four cards per legal slot; stable card identity membership is separate from effective slot metadata | Historical tier embedded in names such as `Root_07_01` is deliberately **not** treated as authoritative. Correctness depends on effective resolver inputs, whose paths/hashes are archived in JSON metadata. |
| **Player count** | Evaluator `--players` -> `MarketEvaluationTelemetry.playerCount` and JSON `experiment.players` -> `ExperimentMetadata.players` -> grouping key in aggregation -> `players` report column | telemetry constructors/comparison enforce same player count; JSON metadata must match comparison | reader positive integer; aggregation rejects mixed counts in single-count API; report separation/sorting test | E2E fixture asserts `4` in final rows | control/learned comparison player counts must match; aggregation partitions by count | Reader accepts any positive count; current confirmation/sanity profiles constrain actual research to 2/3/4. This is intentional package generality, not a current experiment gap. |
| **Learner identity** | Training/evaluation orchestration chooses a weights file -> evaluator JSON `experiment.policyPath` -> `ExperimentMetadata.policy_path` -> `LearnerPerformance.policy_path`; report label is `Path(policy_path).stem` | JSON deterministic fixture includes policy path | reader metadata preservation; report deterministic ordering; **new** duplicate learner result and ambiguous duplicate label rejection | E2E asserts `learner-1` / `learner-2` final rows and aggregate retains exact best purchaser policy path | within one player count, duplicate exact policy path is rejected; duplicate displayed learner label is rejected | Learner identity is provenance-by-policy-path rather than a dedicated numeric learner ID field. Current runner creates unique learner paths, so this is low risk. |
| **Control win share** | `PlayerGameSummary.winShare` for CONTROL affected seat -> telemetry `overallWinShare` -> JSON `outcomes.control.winShare` -> `LearnerResult.win_share` -> raw card report + `learner-win-shares.tsv` | exposure test verifies telemetry average; deterministic JSON fixture verifies exact control value; `GameSummaryExtractorTest` verifies game win shares sum to 1 and losers receive 0 | reader now explicitly asserts exact control win share preservation; report structured outcome/precision tests | E2E final `25.000` rows | role must be `CONTROL`; sample count must match experiment samples; finite numeric value | Python strict reader currently checks finiteness but does not independently clamp to [0,1]. Source semantics and Kotlin tests establish that range; malformed hand-edited JSON could still contain a finite out-of-range value. |
| **Learned win share** | Same path as control, from LEARNED affected seat -> JSON `outcomes.learned.winShare` -> `LearnerResult.win_share` -> learner report and best-purchaser selection | telemetry average and deterministic JSON fixture | reader exact learned value; aggregation best actual purchaser; report outcome/precision tests | E2E final `40.000` / `55.000`; best purchaser aggregate is asserted | role `LEARNED`; sample count matches; finite numeric value | Same finite-but-not-range-clamped JSON risk as control win share. |
| **Learners with purchase** | No Kotlin aggregate. For each validated learner JSON, `card.learned_purchases > 0` -> one `LearnerPerformance` entry -> `CardAggregate.learners_purchasing` -> `learners_with_purchase` | Exact per-card purchase telemetry is Kotlin-covered | focused aggregation test counts only positive-purchase learners; duplicate learner input is now rejected before reporting | E2E card summary checks counts and filtered one-learner report | a learner counts once per card regardless of number of copies bought; duplicate learner result/label rejected | Semantics depend on each JSON representing one independent learner evaluation. Runner/profile tests protect that orchestration assumption. |
| **Best learner using a card** | Among only learners with `learned_purchases > 0`, aggregation selects max `(learned.win_share, policy_path)` -> `CardAggregate.best_purchasing_learner` -> final `best_learner_win_share_pct` | Raw purchase and learned win-share sources covered in Kotlin | focused aggregation test proves high-win non-purchaser is excluded and exact best `policy_path` is retained; report test verifies printed best win share | E2E now asserts exact best purchaser policy path (`learner-2`) and `55.000` final summary value | only actual purchasers eligible; deterministic policy-path tie-break | `card-summary.tsv` currently prints the **best learner's win share but not its identity**. Identity remains available in the aggregate/raw results. If direct identity becomes a routine balance-decision input, add a report column rather than reverse-matching by win share. |
| **Slot totals** | Effective `(type,cost)` groups four `CardAggregate`s -> sums learned exposure/purchases -> `SlotAggregate` -> `slot-summary.tsv` | Effective type/cost source covered | exact slot arithmetic; whole-market reconciliation; report pre-write validation | E2E asserts ROOT/5 final slot line exactly | exactly 4 cards per legal slot; 9 slots; slot exposure/purchase sums must equal member-card sums; market totals must equal slot sums | None identified in current transformation path. `card_learner_rows` is a row-count context field, not an exposure denominator. |
| **Zero-purchase cards** | `CardAggregate.total_purchases == 0` -> `zero-purchase-cards.tsv` | zero-purchase cards remain represented in typed Kotlin telemetry | focused filter test | **Expanded E2E** now asserts a known never-purchased card appears and a purchased card does not | complete card catalog retains zero rows; no dropping zero-purchase cards | None identified in current reporting path. Interpretation remains sensitive to exposure volume, so use alongside exposure/rate columns. |
| **One-learner-only cards** | `CardAggregate.learners_purchasing == 1` -> `one-learner-only-cards.tsv` | per-learner purchase facts covered in Kotlin | focused filter and purchaser-count tests; duplicate learner/label validation prevents false inflation | **Expanded E2E** now asserts known exactly-one-learner cards and excludes a two-learner card | learner counted only when purchases > 0; duplicate learner inputs rejected | Small learner counts can make this category statistically fragile; that is an experiment-size issue, not a reporting correctness issue. |

## Focused tests added during this re-audit

This audit found three useful gaps and closed them:

1. **Full `EvalAccumulator.add()` exposure bridge**
   - The existing Kotlin reconciliation test already proved Chronicle Plant purchases flowed into typed market telemetry.
   - It now also asserts that the same call records the resolved-Grove exposure, sample count, and affected player's win share.
   - This strengthens the state/event -> Kotlin telemetry boundary without running a large simulation.

2. **Duplicate learner protection**
   - Final learner-count metrics could be silently inflated if the same raw learner result was supplied twice.
   - Report validation now rejects duplicate `(playerCount, policyPath)` results.
   - It also rejects distinct policy paths that collapse to the same displayed learner label within a player count.

3. **Expanded end-to-end filtered-report coverage**
   - The deterministic JSON-boundary E2E test already covered exposure, purchases, rates, type/cost, player count, win shares, learner counts, best win share, and slot totals.
   - It now also verifies the final `zero-purchase-cards.tsv` and `one-learner-only-cards.tsv` filters and asserts the exact retained identity of the best purchasing learner in the aggregate.

The Python reader test was also strengthened to assert that **control win share**, not only learned win share, survives JSON loading exactly.

## Regression bugs: current status

### 1. Console indentation made purchases appear all-zero

**Closed architecturally and by regression test.**

The current path is:

```text
GameEntry.Purchase
  -> typed Kotlin purchaseCount
  -> JSON learnedPurchases/controlPurchases
  -> CardResult
  -> reports
```

No final market report parses the human-readable `Individual Plant acquisitions` section. The E2E regression fixture includes intentionally irrelevant console-indentation noise and proves structured nonzero purchases still reach final reports.

### 2. `exposure=0`, `purchases>0`

**Closed by validation at multiple layers.**

The Python reader rejects a positive purchase count with zero exposure before aggregation. Report validation repeats the invariant for typed objects constructed directly in tests/code. The E2E regression test corrupts JSON to this impossible combination and verifies it cannot reach reporting.

## Overall assessment

The current market-reporting pipeline is materially safer than the pre-migration Bash pipeline:

- authoritative market facts are typed before serialization;
- raw JSON is the research-data contract;
- Python validation is strict and fails loudly;
- aggregation is pure and separately tested;
- reports are deterministic and golden-tested;
- known historical reporting failures have explicit regression tests;
- cross-learner card/slot totals reconcile before files are written;
- duplicate learner inputs can no longer silently corrupt learner-count metrics.

The largest remaining structural limitation is not a known correctness defect: there is no single automated test that launches a real Kotlin evaluator and then immediately feeds its generated JSON to Python. Instead, the boundary is covered compositionally by Kotlin telemetry/JSON tests on one side and deterministic raw-JSON-to-final-report E2E tests on the other. That avoids expensive simulations while still testing the research contract directly.

For current market balance work, the final reports are now suitable as the primary reporting source, with the usual distinction that **report correctness does not by itself establish statistical sufficiency of a particular experiment size or strategy population**.
