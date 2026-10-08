# Leaf & Let Die — Market Reporting Pipeline Audit

Date: 2026-10-08

> **Historical audit status**
>
> This document records the pre-migration Bash reporting pipeline and the
> defects that led to its replacement.
>
> The current-market sanity and confirmation experiments no longer use
> `bin/lib/current_market_validation.sh`; that helper has been retired.
> Current market reports consume structured Kotlin JSON through the tested
> Python market package.
>
> **Rule: Human-readable evaluator output is for people. Research reports
> must consume structured machine-readable telemetry.**
>
> References below to Bash parsing, `awk`, `sed`, or
> `bin/lib/current_market_validation.sh` describe the historical pipeline
> being audited, not the current implementation.

## Purpose

This document audits the **current** Plant-market analysis/reporting pipeline after two concrete reporting failures were discovered:

1. Plant purchase totals were reported as zero because Bash parsing expected the wrong indentation around the human-readable `Individual Plant acquisitions` section.
2. `purchases_per_exposure` was invalid for many cards because the Bash report attempted to reconstruct exposure from human-readable evaluator lines that are emitted only for a small `WATCHED_CARDS` subset rather than for all 36 market cards.

The goal of this audit is to define the authoritative data path and the migration boundary before implementation moves important experiment/reporting logic into tested Python under:

```text
src/research/python/leaf_experiments/
```

This is an audit only. No simulator/research behavior is changed by this document.

---

## Executive findings

### 1. The simulator already has most of the important raw facts as typed Kotlin data

The Buy evaluator already accumulates, in typed Kotlin structures:

- affected-role win share;
- Plant purchase count;
- per-card Plant purchase count;
- Plant purchase count by effective cost;
- Plant purchase count by Plant type;
- per-card Grove exposure count (`groveCardGames`);
- per-card win-share sum conditional on Grove presence (`groveCardWins`);
- per-card Plant activations in Cultivation and Battle;
- final Plant count/cost, Plant VP, Battle VP, Wounds, final dice state, and many other research metrics.

The critical exposure count is already correctly accumulated directly from the resolved Grove:

```kotlin
grove.map { it.name }.distinct().forEach { name ->
    groveCardGames[name] = (groveCardGames[name] ?: 0) + 1
    groveCardWins[name] = (groveCardWins[name] ?: 0.0) + p.winShare
}
```

This is the correct authoritative source for card exposure.

### 2. The reporting failure occurs after the typed accumulator

The current market confirmation does **not** consume `groveCardGames` directly.

Instead it:

1. asks `EvaluateBuyPolicyMain.kt` to print human-oriented console prose;
2. parses `Individual Plant acquisitions` with `awk`;
3. parses `Control win share` / `Learned win share` with `sed`;
4. tries to reconstruct Grove exposure from lines of the form:

   ```text
   Root_07_03 present (n=123): ...
   ```

5. aggregates parsed rows into TSV reports using more `awk`.

That is the architectural weak point.

### 3. Complete-market exposure is available internally but not emitted completely

`EvalAccumulator.groveCardGames` tracks all cards appearing in all resolved Groves.

However, the human-readable `present (n=...)` lines are emitted only by `printWatchedCards()`, and only for this hard-coded list:

```text
Root_07_03
Vine_07_01
Vine_07_02
Vine_07_03
Vine_07_04
Vine_09_03
Vine_11_03
Flower_11_01
Flower_14_01
Flower_14_03
Flower_14_04
```

Therefore the current shell reconstruction cannot produce valid exposure values for the rest of the 36-card market.

### 4. The market experiment contains too much research/business logic in Bash

Current sizes:

```text
bin/experiment_current_market_confirmation   464 lines
bin/experiment_current_market_sanity         142 lines
bin/lib/current_market_validation.sh         161 lines
bin/validate_plant_slot_market                82 lines (Python)
```

The confirmation script is no longer merely orchestration. It contains parsing, validation, aggregation, derived metrics, filtering, report construction, resume behavior, seed assignment, and archiving.

This is application/research code and should move into a testable Python package.

### 5. Existing tests protect game mechanics better than report transformation

There is meaningful Kotlin coverage for:

- Buy execution and typed `GameEntry.Purchase` generation;
- random Grove reproducibility and availability;
- training/evaluation Grove-resolution consistency;
- many `EvalAccumulator` helper accumulators;
- GameSummary extraction and win-share invariants.

But there is **no direct focused test** today for:

- `EvalAccumulator.groveCardGames` complete-market accumulation;
- `EvalAccumulator.plantCards` accumulation through `EvalAccumulator.add()`;
- console market-report formatting as a stable contract;
- shell purchase parsing;
- shell exposure parsing;
- `purchases_per_exposure` calculation;
- final per-card/slot TSV aggregation;
- zero-purchase / one-learner filtering;
- end-to-end Chronicle -> evaluator -> report reconciliation.

The current report layer therefore has substantially weaker assurance than the simulator mechanics underneath it.

---

# 1. Current end-to-end data path

## 1.1 Market definition and effective Plant metadata

### Canonical Plant cards

Canonical Plant identity/type/cost originate in:

```text
data/Cultivation_Cards - Root.csv
data/Cultivation_Cards - VF.csv
```

The simulation loads them through `PlantCardRegistry` / `PlantCardManager`.

Each `PlantCard` is a typed Kotlin object containing identity, type, cost, effect/scoring information, etc.

### Research overrides

The market experiments currently default to:

```text
data/research/4p/resync/resync-current.csv
```

`PlantExperimentResearchConfig` / `PlantValueResolver` provide effective research values without modifying canonical data.

### Slot validation

`bin/validate_plant_slot_market` is already a standalone Python program. It:

- reads canonical Root/Vine/Flower CSVs;
- applies effective CSV overrides;
- verifies legal type/cost combinations;
- verifies exactly four available cards in each of the nine legal slots;
- optionally emits card names per slot with `--show-cards`.

The market experiment then parses that human-readable validator output to reconstruct card/type/cost rows.

**Important:** this validator is Python already, but it currently lives directly under `bin/` and no dedicated automated test suite for it was found.

---

## 1.2 Grove construction and exposure

Training and evaluation use the normal typed Grove resolution path.

For random-Grove runs, `--random-grove` maps to `GrovePlantCode.RANDOM_PATTERN` (`000000000`).

The random Grove is resolved using the current Plant catalog plus effective research values.

Relevant current code includes:

```text
src/main/.../plant/GrovePlantResolver.kt
src/simulation/.../learning/buy/TrainBuyPolicyMain.kt
src/simulation/.../learning/buy/EvaluateBuyPolicyMain.kt
```

The evaluation loop resolves one Grove per matched sample and gives the same resolved Grove to CONTROL and LEARNED games.

The affected player's accumulator receives the actual resolved `List<PlantCard>`.

### Authoritative exposure source

Inside `EvalAccumulator.add()`:

```kotlin
grove.map { it.name }.distinct().forEach { name ->
    groveCardGames[name] = (groveCardGames[name] ?: 0) + 1
    groveCardWins[name] = (groveCardWins[name] ?: 0.0) + p.winShare
}
```

Semantics:

- one resolved-Grove exposure per affected-role game/sample;
- a card name is deduplicated with `distinct()` before incrementing;
- `groveCardGames` is the exposure denominator the complete-market report should use;
- `groveCardWins` allows conditional win share given that card was present.

This typed map is **not currently exported as a complete machine-readable report**.

---

## 1.3 Purchase execution and Chronicle data

Plant and die purchases are recorded as typed Chronicle entries:

```kotlin
GameEntry.Purchase(
    ...,
    playerId,
    PurchaseKind.PLANT,
    itemName,
    cost,
    ...
)
```

BuyCoordinator tests exercise actual purchase execution and verify `GameEntry.Purchase` generation.

### Authoritative purchase source in evaluation

`EvalAccumulator.add()` consumes the game's typed Chronicle:

```kotlin
game.entries
    .filterIsInstance<GameEntry.Purchase>()
    .filter { it.playerId == p.playerId }
    .forEach { purchase ->
        when (purchase.kind) {
            PurchaseKind.PLANT -> {
                plantPurchases++
                plantCosts.bump(purchase.cost)
                plantCards.bump(purchase.itemName)
                plantTypes.bump(
                    plantsByName[purchase.itemName]?.type?.name ?: "UNKNOWN"
                )
            }
            PurchaseKind.DIE -> { ... }
        }
    }
```

Thus authoritative purchase facts already exist as typed Kotlin values:

- `plantPurchases` — total Plant purchases;
- `plantCards` — count by exact card identity;
- `plantCosts` — count by effective purchase cost recorded in Chronicle;
- `plantTypes` — count by Plant type resolved from card identity.

---

## 1.4 Player count and learner identity

### Player count

Player count is typed/configured in Kotlin via `EvalOptions.players` and is used to size accumulator seat arrays and game configuration.

It is printed in evaluator startup prose.

The final market TSV does **not** receive player count from a structured evaluator result. Instead, the shell outer loop knows whether it is executing the `2p`, `3p`, or `4p` directory and writes that value into report rows.

### Learner identity

Learner number (`1..5`) is not currently part of `EvaluateBuyPolicyMain`'s domain result.

The shell orchestration derives it from filenames such as:

```text
2p/eval/learner-3.log
```

That shell-derived learner identifier is then inserted into TSV reports.

The trained weights themselves contain policy provenance, but the final TSV learner number is orchestration metadata rather than a typed Kotlin result field.

---

## 1.5 Control and learned win share

`EvalAccumulator.winShare` sums the affected role's typed `PlayerGameSummary.winShare`.

`PlayerGameSummary.winShare` originates in `GameSummaryExtractor`, which is backed by final scoring/winner information.

At report time Kotlin computes:

```kotlin
val cw = c.winShare / o.games
val lw = l.winShare / o.games
```

and prints:

```text
Control win share: ...%
Learned win share: ...%
```

The confirmation Bash script then reparses those lines with `sed` and stores them as percentage-valued strings.

So:

```text
GameSummary.winShare
    -> EvalAccumulator.winShare
    -> formatted console percentage
    -> sed text extraction
    -> TSV percentage
```

The typed authoritative value exists before the text round trip.

---

## 1.6 Plant activation/use telemetry currently available

The Buy evaluator already tracks exact Plant identity for Plant effect resolutions by phase.

`PlantActivationByPhaseAccumulator` consumes typed `GameEntry.EffectResolved` events satisfying:

```text
entry.playerId == affected player
entry.sourceKind == PLANT
```

and records `effect.sourceName` in separate maps for:

- Cultivation;
- Battle.

These are printed as:

```text
Plant activations in Cultivation
Plant activations in Battle
```

This is card-specific use data that the current market TSV pipeline does **not** preserve.

Additional related typed data includes:

- final copies for the hard-coded watched-card set;
- attributed Plant VP for watched cards where scoring is reconstructible;
- total Plant VP in `PlayerGameSummary`;
- final Plant creature signature;
- final Plant count and final printed cost.

A future raw schema can preserve activation counts for all cards without adding new gameplay instrumentation.

---

## 1.7 Evaluator aggregation

The core market evaluator is:

```text
src/simulation/kotlin/dugsolutions/leaf/simulation/v35/learning/buy/EvaluateBuyPolicyMain.kt
```

For every matched sample it:

1. chooses affected seat (`sample % players`);
2. resolves one Grove;
3. runs CONTROL with Human Baseline;
4. runs LEARNED with learned Buy policy only for affected seat;
5. uses same Grove/mechanical/strategy schedules for the matched pair;
6. extracts a `GameSummary` and Chronicle entries;
7. accumulates CONTROL and LEARNED separately in `EvalAccumulator`;
8. renders a large human-readable report with `printReport()`.

`EvalAccumulator` is already the natural typed boundary from which a machine-readable report should be constructed.

---

## 1.8 Console output

Relevant human-readable sections include:

```text
Held-out result
  Control win share: ...
  Learned win share: ...

Buy behavior ...
  Plant purchases: ...
  Plant purchases by effective cost
  Plant purchases by type
  Individual Plant acquisitions
  Plant activations in Cultivation
  Plant activations in Battle

Grove sensitivity and notable VP cards
  <WATCHED_CARD> present (n=...): ...
```

The console output is useful for humans, but it is not a stable data-transfer API.

Two report bugs already resulted from treating it as one.

---

## 1.9 Shell parsing and aggregation

Current long confirmation:

```text
bin/experiment_current_market_confirmation
```

Current shared helpers:

```text
bin/lib/current_market_validation.sh
```

The confirmation script currently overrides some helper functions locally because the original helper parser was already found to be wrong.

### Purchase parsing

Current fixed parser searches between textual headers:

```text
Individual Plant acquisitions:
...
Plant activations in Cultivation:
```

and parses lines such as:

```text
Card_Name: control=N learned=N
```

The earlier shared helper required exact indentation and silently missed the section.

### Exposure parsing

Current parser searches console lines matching:

```text
Card_Name present (n=N):
```

Those lines come only from `WATCHED_CARDS`.

Therefore this parser cannot produce complete-market exposure data.

### Derived purchase/exposure ratio

The Bash script computes:

```text
learned_purchases / grove_exposures
```

but currently writes `0.0` when exposure is zero.

For cards whose exposure is absent only because the console never printed it, that transforms **missing data into a false numerical zero**.

This is the second concrete reporting failure.

---

# 2. Typed Kotlin vs reconstructed/parsing-derived data

| Datum | Already typed before reporting? | Current authoritative source | Current report path |
|---|---:|---|---|
| Player count | Yes | `EvalOptions.players` / game config | Reintroduced by shell loop |
| Learner number | No evaluator-domain field | experiment orchestration / filename | shell filename parsing |
| Policy provenance | Yes | `LearnedBuyWeights.provenance` | console only, not current TSV |
| Card identity | Yes | `PlantCard.name`, `GameEntry.Purchase.itemName` | purchases parsed from console; complete list parsed from validator prose |
| Plant type | Yes | `PlantCard.type` | shell parses slot-validator prose |
| Effective Plant cost | Yes | effective Plant values + `Purchase.cost` | shell parses slot-validator prose for card metadata; purchase accumulator also has cost buckets |
| Grove exposure by card | **Yes** | `EvalAccumulator.groveCardGames` | **incorrectly reconstructed from watched-card prose** |
| Conditional win share by Grove presence | Yes | `groveCardWins / groveCardGames` | human watched-card prose only |
| Control purchases by card | Yes | control `EvalAccumulator.plantCards` | parsed from console |
| Learned purchases by card | Yes | learned `EvalAccumulator.plantCards` | parsed from console |
| Total Plant purchases | Yes | `EvalAccumulator.plantPurchases` | console only |
| Purchases by cost | Yes | `EvalAccumulator.plantCosts` | console only |
| Purchases by type | Yes | `EvalAccumulator.plantTypes` | console only |
| Cultivation Plant activations by card | Yes | `PlantActivationByPhaseAccumulator.cultivation` | console only |
| Battle Plant activations by card | Yes | `PlantActivationByPhaseAccumulator.battle` | console only |
| Control win share | Yes | `control.winShare / games` | formatted then reparsed with `sed` |
| Learned win share | Yes | `learned.winShare / games` | formatted then reparsed with `sed` |
| Purchases per exposure | Derived | exact purchases + exact exposure | currently derived in Bash from one good and one incomplete parsed input |
| Learners evaluated | Derived orchestration | count of learner result rows | `awk` aggregation |
| Learners with purchase | Derived | per-learner learned purchases | `awk` aggregation |
| Best learner win share | Derived | per-learner learned win share | `awk` aggregation |
| Slot purchase/exposure totals | Derived | card-level typed facts + market metadata | `awk` aggregation |
| Zero-purchase classification | Derived | aggregate learned purchase count | `awk` filter |
| One-learner-only classification | Derived | learners-with-purchase | `awk` filter |

Key conclusion:

> The central raw facts are mostly already typed. The migration does not require rediscovering the data. It requires exporting the typed facts directly and moving derived reporting into tested code.

---

# 3. Current TSV schemas and authoritative sources

## 3.1 `card-purchases.tsv`

Current header:

```text
players
learner
control_win_share_pct
held_out_win_share_pct
card
type
cost
grove_exposures
control_purchases
learned_purchases
learned_purchases_per_exposure
```

| Field | Current authoritative source | Current transformation | Current test coverage |
|---|---|---|---|
| `players` | experiment's player-count loop / Kotlin `EvalOptions.players` | shell inserts loop variable | Kotlin options/player-count tests exist; final shell insertion untested |
| `learner` | orchestration identity | parsed from `learner-N.log` filename | No direct report-layer test |
| `control_win_share_pct` | CONTROL `EvalAccumulator.winShare / games` | Kotlin formats percentage; shell reparses text | `GameSummaryExtractor` win-share invariants tested; text round-trip untested |
| `held_out_win_share_pct` | LEARNED `EvalAccumulator.winShare / games` | same | same |
| `card` | typed Plant card identity | shell parses `validate_plant_slot_market --show-cards` prose | Plant registry/resolver tests exist; shell metadata extraction untested |
| `type` | typed `PlantCard.type` / effective market | shell parses slot-validator prose | Plant/slot resolver tested; final parser untested |
| `cost` | typed/effective Plant cost | shell parses slot-validator prose | effective Plant override tests exist; final parser untested |
| `grove_exposures` | **`EvalAccumulator.groveCardGames[card]`** | current shell incorrectly reconstructs from watched-card lines | **No focused test of complete-map accumulation; shell parser untested** |
| `control_purchases` | CONTROL `plantCards[card]` | console text -> awk | purchase mechanics tested; `EvalAccumulator.add()` card count not directly tested; parser untested |
| `learned_purchases` | LEARNED `plantCards[card]` | console text -> awk | same |
| `learned_purchases_per_exposure` | learned purchases / exposure | Bash arithmetic | **No unit test; currently invalid when exposure text is absent** |

### Current trust assessment

- purchase totals: plausible/grounded in typed Chronicle but report transformation is insufficiently tested;
- exposure: typed source is good, current TSV extraction is not;
- purchase/exposure: not trustworthy in current final report.

---

## 3.2 `card-summary.tsv`

Current header:

```text
players
card
type
cost
total_grove_exposures
total_learned_purchases
purchases_per_exposure
learners_evaluated
learners_with_purchase
best_learner_win_share_pct
```

| Field | Authoritative source | Current derivation | Test coverage |
|---|---|---|---|
| `players` | per-learner row | grouping key in awk | none at report layer |
| `card` | typed card identity | grouping key | none at report layer |
| `type` | typed/effective card type | grouping key | none at report layer |
| `cost` | typed/effective cost | grouping key | none at report layer |
| `total_grove_exposures` | sum of exact learner exposures | sums currently incomplete TSV exposure | none; currently unreliable |
| `total_learned_purchases` | sum of learner `plantCards` counts | awk sum | none |
| `purchases_per_exposure` | total purchases / total exposure | awk arithmetic | none; inherits exposure defect |
| `learners_evaluated` | number of card rows | awk count | none |
| `learners_with_purchase` | learner rows with purchases > 0 | awk count | none |
| `best_learner_win_share_pct` | maximum learned win share among rows | awk max | none; note current implementation does **not** require that learner to have purchased the card despite report interpretation sometimes implying this |

### Additional semantic issue

The current code updates `bestWin[key]` for every learner row, regardless of whether that learner purchased the card. Therefore the field is currently better described as:

```text
best evaluated learner win share for this player-count/card row set
```

not necessarily:

```text
best learner that used/purchased this card
```

A typed Python redesign should define and test the intended semantic explicitly.

---

## 3.3 `slot-summary.tsv`

Current header:

```text
players
type
cost
total_grove_exposures
total_learned_purchases
purchases_per_exposure
card_learner_rows
```

| Field | Authoritative source | Current derivation | Test coverage |
|---|---|---|---|
| `players` | per-card rows | grouping key | none |
| `type` | effective card metadata | grouping key | none |
| `cost` | effective card metadata | grouping key | none |
| `total_grove_exposures` | sum card exposures in slot | awk sum | none; currently inherits incomplete exposure |
| `total_learned_purchases` | sum card purchases in slot | awk sum | none |
| `purchases_per_exposure` | slot purchases / summed card exposures | awk arithmetic | none |
| `card_learner_rows` | count of input rows in group | awk count | none |

The future implementation should test the invariant:

```text
slot total == sum of the four member-card totals
```

for both purchases and exposure.

---

## 3.4 `zero-purchase-cards.tsv`

Schema is copied from `card-summary.tsv`.

Rows are filtered where:

```text
total_learned_purchases == 0
```

Authoritative fact is aggregate learned card-purchase count.

Current filter is pure Bash/awk and has no focused automated test.

The earlier all-zero parser bug caused this report to falsely classify the complete market as zero-purchase, demonstrating the need for explicit regression coverage.

---

## 3.5 `one-learner-only-cards.tsv`

Schema is copied from `card-summary.tsv`.

Rows are filtered where:

```text
learners_with_purchase == 1
```

This is wholly derived reporting logic and currently untested.

A future test should distinguish:

- five learners evaluated / one bought;
- one learner evaluated / one bought;
- five learners evaluated / zero bought.

---

## 3.6 `vine-9-summary.tsv`

Schema is copied from `card-summary.tsv`.

Rows are filtered where:

```text
type == VINE
cost == 9
```

The filter is straightforward but currently untested.

This should become a normal reusable query/filter in Python rather than a special `awk` fragment.

---

## 3.7 `learner-win-shares.tsv`

Current header:

```text
players
learner
control_win_share_pct
held_out_win_share_pct
```

| Field | Authoritative source | Current path | Test coverage |
|---|---|---|---|
| `players` | orchestration/Kotlin options | shell loop | options tested, final report untested |
| `learner` | orchestration identity | filename | none |
| `control_win_share_pct` | CONTROL accumulator | Kotlin formatted prose -> sed -> per-card TSV -> de-dup | core win-share invariant tested; report path untested |
| `held_out_win_share_pct` | LEARNED accumulator | same | same |

Notably, this learner-level report currently derives its values indirectly from duplicated per-card rows instead of consuming a learner-level structured result.

---

# 4. Relevant existing Kotlin test coverage

## 4.1 Buy mechanics / purchase Chronicle

### `src/test/.../game/buy/BuyCoordinatorTest.kt`

This is substantial game-mechanics coverage around Buy execution. It includes many tests involving actual purchasing and assertions on typed `GameEntry.Purchase` entries.

This gives good confidence that legal purchases become Chronicle purchase events.

It does **not** by itself prove that `EvalAccumulator.plantCards` or downstream TSV reporting counts those events correctly.

### `src/integration/.../sanity/cultivation/CultivationBuySanityTest.kt`

Integration-level sanity checks verify actual Cultivation Buy behavior and purchase kind.

Again, this is upstream of the market report transformation.

---

## 4.2 Buy evaluator tests

### `src/simulationTest/.../learning/buy/EvaluateBuyPolicyTest.kt`

Current relevant tests include:

- evaluator policy-option defaults;
- accumulator starts empty / seat bucket sizing;
- Plant activation research separates Cultivation and Battle by card;
- 2p/3p/4p evaluator player count handling;
- Buy-phase shape grouping and zero-purchase phases;
- round-effect opportunity/use distinction;
- die-development provenance;
- strike research;
- research-environment controls and reproducibility.

Important direct gap:

> No test was found that constructs a `CompletedEvalGame`, calls `EvalAccumulator.add()`, and asserts exact values for `plantCards`, `plantCosts`, `plantTypes`, `plantPurchases`, `groveCardGames`, and `groveCardWins` together.

That is the single most relevant missing Kotlin unit-test seam for current market reporting.

---

## 4.3 Random Grove / resolver tests

### `src/simulationTest/.../learning/buy/TrainBuyPolicyGroveTest.kt`

Relevant tests include:

- training defaults;
- 2/3/4 player handling;
- random Grove is reproducible and respects Plant availability;
- fixed Grove rejects unavailable requested card;
- training and evaluation share Grove-resolution semantics.

This is good coverage for Grove generation itself.

It does not verify that evaluator exposure counters later report all resolved Grove cards correctly.

### `src/test/.../plant/GrovePlantResolverTest.kt`

Contains broad lower-level tests for Grove-slot resolution and legal Plant selection.

This gives strong upstream confidence in the nine-slot Grove mechanism.

---

## 4.4 Game summary / win share

### `src/simulationTest/.../analysis/GameSummaryExtractorTest.kt`

This verifies a real completed game collapses into compact typed summaries and includes checks for:

- player identity/seat;
- total VP;
- Plant VP;
- final Plant count/cost;
- final Plant identity/signature;
- final dice count/power/signature;
- Battle Strike VP;
- Wounds;
- Wisp metrics;
- shared token economy;
- win shares sum to exactly `1.0`;
- winners have positive share and non-winners zero share.

This is meaningful coverage for the typed source underlying control/learned win share.

It does not cover the console formatting -> shell extraction -> TSV round trip.

---

## 4.5 Plant activation telemetry

`EvaluateBuyPolicyTest` directly tests `PlantActivationByPhaseAccumulator` with typed `EffectResolved` events and asserts exact Cultivation/Battle counts by Plant identity.

This is stronger coverage than exists for card purchase/exposure accumulation.

---

## 4.6 Plant research override metadata

Relevant tests include:

```text
PlantExperimentConfigLoaderTest
PlantExperimentConfigTest
PlantExperimentEffectiveValuesTest
PlantExperimentScoringIntegrationTest
```

These protect effective research values and override behavior.

No test was found for the standalone Python `bin/validate_plant_slot_market` program or for parsing its `--show-cards` console output into the final market report.

---

# 5. Coverage matrix for current final-report fields

Legend:

- **Strong** — direct typed-source behavior has focused tests.
- **Partial** — upstream mechanics tested, but this accumulator/report seam is not directly tested.
- **None** — no focused automated test found for the field transformation in question.
- **Broken path** — current transformation is known to be incomplete/invalid.

| Final field | Typed source | Typed-source coverage | Report transformation coverage | Overall status |
|---|---|---|---|---|
| players | `EvalOptions.players` | Strong | None | Partial |
| learner | orchestration metadata | n/a | None | Weak |
| control win share | `PlayerGameSummary.winShare` -> accumulator | Strong upstream | None | Partial |
| learned win share | same | Strong upstream | None | Partial |
| card identity | `PlantCard.name` / purchase `itemName` | Strong upstream | None | Partial |
| type | `PlantCard.type` | Strong upstream | None | Partial |
| cost | effective Plant/card/purchase cost | Strong/partial upstream | None | Partial |
| grove exposure | `EvalAccumulator.groveCardGames` | **No focused accumulator test** | **Known broken parser** | **Broken path** |
| control card purchases | `control.plantCards` | upstream purchase strong; accumulator add untested | None | Partial/weak |
| learned card purchases | `learned.plantCards` | same | parser already failed once | Partial/weak |
| purchases/exposure | derived | n/a | None | **Broken path** |
| learners evaluated | derived | n/a | None | Weak |
| learners with purchase | derived | n/a | None | Weak |
| best learner win share | derived | n/a | None; semantic ambiguity | Weak |
| slot purchases | derived | n/a | None | Weak |
| slot exposure | derived | n/a | None; inherits broken exposure | Broken path |
| zero-purchase classification | derived | n/a | None; prior regression observed | Weak |
| one-learner classification | derived | n/a | None | Weak |
| Vine-9 subset | derived | n/a | None | Weak |

---

# 6. Recommended machine-readable Kotlin raw-result schema

## 6.1 Boundary recommendation

The machine-readable result should be produced directly from typed evaluator accumulators, **before human-oriented formatting**.

Recommended flow:

```text
Game + Chronicle
    -> GameSummaryExtractor / EvalAccumulator
    -> typed MarketEvaluationRawResult
    -> JSON
    -> tested Python loader/aggregation/reporting
```

Human console rendering should remain available but should not be an input to research reports.

## 6.2 Recommended schema shape

JSON is recommended because:

- nested control/learned structures are natural;
- schema/version metadata is easy;
- Kotlin and Python both handle it cleanly;
- missing vs zero can be represented explicitly;
- report generation need not depend on column ordering;
- future telemetry can be added compatibly.

Suggested logical schema:

```json
{
  "schema": "leaf.market-evaluation",
  "schemaVersion": 1,
  "experiment": {
    "players": 4,
    "matchedSamples": 2500,
    "roundPattern": "3/2/2",
    "groveMode": "RANDOM_PATTERN",
    "mechanicalSeedStart": 4410000,
    "strategySeedStart": 4510000,
    "groveSeedStart": 4610000,
    "plantOverrides": "data/research/4p/resync/resync-current.csv",
    "plantFingerprint": "...",
    "roundOverrides": "data/research/4p/resync/round-resync-current.csv",
    "roundFingerprint": "..."
  },
  "policy": {
    "weightsPath": ".../learner-2.weights",
    "policyFingerprint": "...",
    "trainingProvenance": { }
  },
  "control": {
    "winShare": 0.2484,
    "plantPurchases": 1234,
    "plantActivations": {
      "cultivation": { "Root_05_01": 10 },
      "battle": { "Root_05_01": 4 }
    }
  },
  "learned": {
    "winShare": 0.4756,
    "plantPurchases": 1740,
    "plantActivations": {
      "cultivation": { "Root_05_01": 12 },
      "battle": { "Root_05_01": 8 }
    }
  },
  "cards": [
    {
      "cardId": "Root_05_01",
      "type": "ROOT",
      "effectiveCost": 5,
      "available": true,
      "slot": { "type": "ROOT", "cost": 5 },
      "groveExposures": 612,
      "control": {
        "purchases": 144,
        "grovePresenceWinShareSum": 152.5,
        "cultivationActivations": 30,
        "battleActivations": 8
      },
      "learned": {
        "purchases": 278,
        "grovePresenceWinShareSum": 182.0,
        "cultivationActivations": 41,
        "battleActivations": 11
      }
    }
  ]
}
```

The exact Kotlin DTO names are implementation choices, but these semantics should be preserved.

## 6.3 Raw facts that should be serialized, not derived downstream in Kotlin prose

Required for current market reports:

- player count;
- matched sample count;
- policy/learner identity or enough provenance for orchestration to attach it;
- control win share as raw decimal;
- learned win share as raw decimal;
- complete effective market card catalog;
- exact identity/type/effective cost for every card;
- exact Grove exposure count for every card;
- exact control purchases for every card;
- exact learned purchases for every card.

Recommended because data already exists and is valuable for card diagnosis:

- Cultivation activation count by card;
- Battle activation count by card;
- Grove-presence win-share sum or conditional win share for both conditions;
- total Plant purchases;
- purchases by type;
- purchases by cost;
- final copies where practical for all cards rather than only watched cards.

## 6.4 Derived metrics that should belong primarily in Python

Python should calculate and test:

- purchases per exposure;
- learner counts;
- learners with purchase;
- best learner that actually purchased/used a card;
- slot totals/rates;
- zero-purchase classifications;
- one-learner-only classifications;
- cross-player-count comparison tables.

This keeps Kotlin responsible for simulation truth and Python responsible for analysis/report presentation.

## 6.5 Required schema invariants

At serialization or Python validation boundaries, assert:

1. player count is supported (`2..4`);
2. matched sample count is positive;
3. every complete-market card identity is unique;
4. exactly 36 available cards exist in complete-market mode;
5. each legal slot contains exactly four cards;
6. card exposure is `0..matchedSamples`;
7. purchases are non-negative;
8. if purchases > 0, exposure must be > 0 for random-Grove market evaluation;
9. sum of exact per-card purchases equals total Plant purchases for the same condition;
10. control and learned card metadata describe the same effective market;
11. raw win shares are finite and `0.0..1.0`.

These invariants would have converted both known reporting bugs from silent bad data into loud failures.

---

# 7. Recommended Python package/test structure

Recommended project layout:

```text
src/
  research/
    python/
      leaf_experiments/
        __init__.py

        common/
          __init__.py
          project_paths.py
          process.py
          timing.py
          resume.py
          archive.py

        market/
          __init__.py
          models.py
          raw_result.py
          validation.py
          aggregation.py
          reports.py
          runner.py
          profiles.py
          cli.py

      tests/
        conftest.py

        market/
          test_raw_result.py
          test_validation.py
          test_aggregation.py
          test_reports.py
          test_runner.py
          test_end_to_end.py

          fixtures/
            valid_market_result.json
            malformed_market_result.json
            multi_learner_results/
```

## Module responsibilities

### `market/models.py`

Python dataclasses / immutable domain objects:

- experiment metadata;
- policy/learner metadata;
- card/slot metadata;
- condition metrics;
- raw learner evaluation;
- aggregate card result;
- aggregate slot result.

No file I/O or subprocess execution.

### `market/raw_result.py`

- JSON loading;
- schema-version dispatch;
- conversion to typed Python domain models.

### `market/validation.py`

Pure validation/invariants:

- complete 36-card market;
- four-per-slot;
- nonnegative metrics;
- exposure bounds;
- purchase/exposure consistency;
- card/slot uniqueness;
- reconciliation checks.

### `market/aggregation.py`

Pure transformations:

- aggregate learners;
- purchases/exposure;
- slot rollups;
- zero-purchase classifications;
- one-learner-only classifications;
- successful-learner/card association.

### `market/reports.py`

Deterministic rendering of:

- `card-purchases.tsv`;
- `card-summary.tsv`;
- `slot-summary.tsv`;
- `zero-purchase-cards.tsv`;
- `one-learner-only-cards.tsv`;
- `vine-9-summary.tsv`;
- `learner-win-shares.tsv`;
- `README-FIRST.txt`.

No subprocess execution.

### `market/runner.py`

Experiment orchestration:

- training/evaluation commands;
- deterministic seed allocation;
- result paths;
- resume markers;
- failure handling;
- progress/ETA;
- archive preparation.

It should consume machine-readable evaluator files rather than parse stdout.

### `market/profiles.py`

Named profiles such as:

```text
sanity
confirmation
```

so the two public experiments share one implementation.

### `market/cli.py`

Argument parsing and entry point only.

---

# 8. Recommended public launchers

Preserve the user's familiar commands:

```text
bin/experiment_current_market_sanity
bin/experiment_current_market_confirmation
```

but reduce them to tiny launchers, approximately:

```bash
#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PYTHONPATH="$ROOT/src/research/python${PYTHONPATH:+:$PYTHONPATH}"
exec python3 -m leaf_experiments.market.cli sanity "$@"
```

and equivalent for `confirmation`.

Bash remains appropriate for this tiny process-launching seam.

---

# 9. Recommended test strategy

## Kotlin responsibility tests

Kotlin should prove that the machine-readable raw facts match simulation facts.

Priority additions:

1. `EvalAccumulator.add()` exact Plant purchases by card/type/cost.
2. `EvalAccumulator.add()` exact Grove exposures for multiple cards.
3. duplicate card identity in input Grove does not double-count an exposure because of `distinct()`.
4. exact `groveCardWins` accumulation.
5. all market cards can be represented with explicit zero purchases.
6. raw DTO creation from accumulators reconciles total Plant purchases.
7. serializer preserves exact integer counts and decimal win-share data.

## Python responsibility tests

Python should prove analysis arithmetic/report construction.

Priority tests:

- schema-version validation;
- complete market validation;
- exact purchases/exposure arithmetic;
- zero denominator -> unavailable, not fabricated `0.0`;
- multi-learner aggregation;
- learners-with-purchase;
- best **purchasing** learner semantics;
- slot reconciliation;
- deterministic report ordering;
- exact TSV golden files;
- zero-purchase and one-learner filters;
- Vine-9 filter;
- runner command construction/resume/error propagation.

## End-to-end regression fixture

A small deterministic raw fixture should explicitly prove:

```text
Card A: exposure=3, purchases=2 -> rate=0.666666...
Card B: exposure=2, purchases=0 -> rate=0
Card C: exposure=1, purchases=2 -> rate=2.0
```

and assert that:

- purchase parser indentation cannot affect results because no prose is parsed;
- `exposure=0 && purchases>0` fails loudly.

---

# 10. Specific current-source risks to preserve in migration planning

## 10.1 `best_learner_win_share_pct` semantics need correction/definition

The current shell implementation records the maximum learner win share for each card row regardless of whether that learner actually purchased the card.

If the intended metric is "best successful learner that values this card," the future Python version should require a positive purchase/use condition and give the field a precise name.

## 10.2 Effective cost/type must come from one authoritative market catalog

Today report metadata is reconstructed from the output of another executable (`validate_plant_slot_market`).

The future raw Kotlin result should serialize the exact effective card metadata used by the evaluator itself. Python can independently validate the legal 4x9 shape, but should not need to reconstruct simulation metadata from human prose.

## 10.3 Exposure semantics should be named explicitly

Current `groveCardGames` means:

> number of affected-role evaluated games/samples whose resolved Grove contained the card at least once.

This is not the same thing as:

- number of times a Grove slot was inspected;
- number of purchase opportunities;
- number of Buy phases in which the card was present and affordable;
- number of physical copies present.

The raw schema should name it `groveExposures` or `gamesPresent`, and documentation should preserve this meaning.

## 10.4 Purchases per Grove exposure is not purchase probability

A player may buy multiple copies over a game, so:

```text
purchases / Grove exposure
```

can exceed `1.0`.

It is a useful demand-intensity metric, not a probability.

## 10.5 Card activation count is not activation opportunity rate

The existing activation maps count resolved Plant effects by identity and phase.

They do not currently provide the denominator "times this owned face-up card could legally have been activated."

Do not label raw activation counts as activation/opportunity rates unless a separate opportunity metric is added.

---

# 11. Recommended migration order

The safest migration sequence is:

1. add focused Kotlin accumulator tests;
2. define typed raw market DTO/schema;
3. add optional machine-readable evaluator output;
4. add serializer/schema tests;
5. establish Python package/test skeleton;
6. implement strict Python JSON loader/models;
7. implement pure market aggregation + tests;
8. implement deterministic report writers + golden tests;
9. migrate confirmation orchestration to Python;
10. migrate sanity profile to the same runner;
11. add end-to-end fixture/regression tests;
12. retire old shell parsing only after equivalence is demonstrated.

Do **not** delete the human-readable evaluator report. It remains useful diagnostically. It should simply cease being a data API.

---

# 12. Bottom-line audit conclusion

The current simulator does **not** need a wholesale rewrite to make market reporting trustworthy.

The strongest conclusion from source inspection is:

> Most authoritative market facts already exist in typed Kotlin before reporting. The fragile layer is the conversion of those facts to console prose and then back into data with Bash/awk/sed.

The most important architectural change is therefore to establish a direct structured boundary:

```text
typed Kotlin accumulator
    -> versioned machine-readable result
    -> validated/tested Python analysis
    -> deterministic reports
```

This will allow market-design decisions to rely on report fields that have explicit semantics, invariants, unit tests, and end-to-end regression protection.

