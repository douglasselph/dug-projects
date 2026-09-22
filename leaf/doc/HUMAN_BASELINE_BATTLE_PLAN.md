# Human Baseline Battle Implementation Plan

This document turns the approved Battle behavior contract into small implementation checkpoints.

The behavior contract is [`HUMAN_BASELINE_BATTLE.md`](HUMAN_BASELINE_BATTLE.md). Overall Milestone-2 certification state is tracked in [`HUMAN_BASELINE.md`](HUMAN_BASELINE.md).

## Status

Battle Stage A is complete:

- [x] A1 — current Battle inventory
- [x] A2 — existing Row Need, scoring, Support, Effect, and policy inventory
- [x] A3 — row-state / Score-Benchmark / Live-Threat / Done contract
- [x] A4 — Battle Swing / transition contract
- [x] A5 — First Main / random-information contract
- [x] A6 — Support / Final Main / continuation / tempo contract

Stage B has begun.

- [x] B1 — durable Battle plan + policy foundation
- [x] B2 — Done state in engine / decision context
- [x] B3 — Battle Step-5 loop safety
- [x] B4 — `BattleRowAssessor`
- [x] B5 — `BattleSwingEvaluator`
- [x] B6 — `BattleVpImpact` + `BattleActionAnalyzer`
- [x] B7 — First Main
- [x] B8 — actual Battle die placement
- [x] B9 — Support capacity
- [x] B10 — Support reachability + continuation
- [x] B11 — direct Support analysis
- [ ] B12 — enabling Support analysis
- [ ] B13 — special Battle effect evaluators
- [ ] B14 — Support vs Final Main orchestration
- [ ] B15 — action / target / branch alignment
- [ ] B16 — single-purpose helper API cleanup + behavior-contract tests
- [ ] B17 — focused compile / test / fix

Stage C:

- [ ] C1 — durable certification documentation
- [ ] C2 — full `test integrationTest simulationCheck` regression + Markdown-link verification
- [ ] C3 — mark Battle CERTIFIED and package final patch

B1 intentionally changes no tactical Battle behavior.

B2 moves Done into authoritative Battle-round state and exposes it through immutable decision context. It still does not implement tempo intelligence or tactical Battle scoring.

B3 adds independent caller safety to the repeated Step-5 loop. It rejects a repeated per-player decision state and also enforces a generous per-player hard ceiling, while treating real changes made by other players as legitimate progress.

B4 adds the shared multiplayer-aware `BattleRowAssessor`. It separates actual Strike winner/Score-Benchmark facts from Live-Threat/Done information and derives temporary `securedForNow` / `potentiallyHopeless` observations through `HumanBaselinePolicy`.

B5 adds `BattleSwingEvaluator`, the shared factual tactical valuation layer. It implements the 500/400/300/200/100 transition hierarchy from the policy transition scale, one strongest transition base per row, raw margin movement, symmetric reverse-scored harm, uncapped multi-row aggregation, and `Double` expected values. It does not yet change First Main, Support, Effect targeting, or actual die-placement behavior.

B6 adds `BattleVpImpact` and `BattleActionAnalyzer`. `BattleVpImpact` projects the actor's real immediate Strike VP using the same base-2-plus-wounds semantics as `StrikeResolver`. `BattleActionAnalyzer` combines row-level Battle Swing, projected Strike-VP change, and the separate positive-boundary `improvementStepCount` needed by later premium-resource gates. Candidate realizations explicitly record whether they are deterministic, expected-before-RNG, or actual-after-RNG. The layer remains factual: it does not yet decide whether to spend a resource, delay Final Main, or prefer one intrinsic card value over another.

B7 connects the shared tactical layer to Step-4 First Main. Draw now evaluates the known next die by expected roll and best currently legal expected placement, without consuming mechanical RNG or pre-committing the eventual row. Existing Plant/Round intrinsic scores remain intact; target-dependent Plant/Effect tactical projection stays intentionally deferred to B15 so the top-level action and downstream legal target request can share one implementation rather than duplicate target rules.

B8 routes actual post-roll placement through that same shared tactical layer. `BattlePlacementPriority` evaluates the known rolled value in `ACTUAL` mode against each row in the current request and fresh Battle snapshot. `BaselineScoreEngine` uses `StrategyRandomizer` only when the resulting Battle Swing scores tie exactly. Existing rule-mandated placement remains a direct engine operation with no strategy call.

B9 adds reusable `BattleSupportCapacityAssessor` and `BattleSupportCapacity` concepts. Own capacity is normalized from current legal Step-5 choices so targets do not multiply a resource's move count. Opponent capacity uses public board state only, treats hidden Wisp identities as unknown, excludes unusable visible resources and Done players, and reports the maximum among a caller-supplied relevant Live-Threat set rather than summing opponents as a coalition. Identifying close rows and the relevant opponent set remains a later continuation/tempo checkpoint.

B10 adds `BattleSupportReachability`, `BattleContinuationAssessor`, and the immutable assessment results consumed by later Support orchestration. Reachability projects the actor's normalized legal Bee capacity at current Bee value plus expected keep-better Butterfly improvement, caps repeated reroll potential at visible die headroom, and deliberately excludes Worm, Water, Mulch, and Wisp from generic additive power. The continuation gate uses the policy-supplied minimum meaningful Strike-VP gain and succeeds for either an individually worthwhile Support or a meaningful cumulative path. B10 does not yet score individual Supports or select the actual Support/Final-Main action; B11/B12 and B14 own those layers.

B11 adds `BattleDirectSupportAnalyzer` and routes ordinary direct Support priority through the shared tactical layer. Bee and direct Worm placement use current round-modified values; Butterfly uses expected keep-better gain on its exact die and row; Water reroll uses expected replacement value and requires a positive named transition; Mulch uses expected stored-die roll plus the best currently legal placement and requires `WIN_FLIPPED`. Hypothetical analysis consumes no mechanical RNG. Pre-Battle Critter reserves are not applied during Battle. Ordinary Wisps retain card-specific intrinsic scoring, while the cross-player swap, immediate resolution, and two-step upgrade target remain explicitly deferred to B13. Worm commitment/enabling rules remain B12, and B14 still owns Support-versus-Final-Main orchestration.

B1 verification:

```text
compileKotlin + compileTestKotlin --rerun-tasks
    BUILD SUCCESSFUL

focused:
    HumanBaselinePolicyTest
    HumanBaselineDecisionDirectorTest
    HumanBaselineBattleStrategyTest

    13 tests
    0 failures
    0 errors
    0 skipped

relative Markdown-link issues
    0
```

## B1 — durable Battle plan + policy foundation

B1 establishes the durable contract, KDoc navigation, and a shared overridable policy seam.

Confirmed policy defaults:

| Policy | Default |
|---|---:|
| Battle transition scale | 100 |
| Close margin | 4 |
| Secured lead | 10 |
| Potentially hopeless deficit | 10 |
| Minimum meaningful Strike-VP gain | 2 |
| Water Refresh minimum improvement steps | 2 |
| Immediate-resolve Wisp minimum lead | 5 |
| Immediate-resolve Wisp minimum opponent Support | 3 |

`HumanBaselineBattleStrategy` now receives the same `HumanBaselinePolicy` instance supplied by `HumanBaselineDecisionDirector`. Later checkpoints must consume the overridable methods rather than reading companion constants directly.

## B2 — Done state in engine / decision context

**COMPLETE.**

Done is now authoritative player-level state on `BattleState` rather than a coordinator-local active-set fact. `BattleState` exposes immutable `donePlayerIds`, `isDone(playerId)`, and a guarded `markDone(playerId)` mutation API.

`BattleView` copies `donePlayerIds` into every Battle `DecisionContext`, so strategies can observe who can no longer take Battle turns without holding private strategy memory or a live mutable state reference.

`BattleActionCoordinator` now uses `BattleState.isDone(...)` to skip completed players and calls `markDone(...)` only **after** the selected Final Main Action has completely returned from execution. A Done player still participates in Strike state and remains targetable according to normal effect legality; B2 does not equate Done with withdrawal.

Focused tests cover:

- fresh/monotonic Done state and invalid duplicate/non-participant completion;
- immutable Done snapshots in `DecisionContext`;
- later Step-5 requests observing opponents who have become Done;
- Final Main effect execution observing the actor as not-yet-Done until resolution returns.

B2 intentionally does not implement tempo intelligence yet.

B2 verification:

```text
compileKotlin
    BUILD SUCCESSFUL

compileTestKotlin
    BUILD SUCCESSFUL

focused:
    BattleStateTest
    DecisionContextFactoryTest
    BattleActionCoordinatorTest

    14 tests
    0 failures
    0 errors
    0 skipped
```

## B3 — Battle Step-5 loop safety

**COMPLETE.**

`BattleActionCoordinator` now maintains a separate Step-5 safety history and decision count for each Battle player. Before each still-active player's Step-5 choice it records:

- the currently legal Support / Final Main choices; and
- a stable `DecisionContext` observation for **every** Battle player.

Using all players' observations means legitimate changes in another player's private Wisp identity still count as progress to the coordinator even though that identity remains hidden from the acting strategy. The pass number is intentionally excluded: advancing to another pass without any legal-choice or game-state change is not progress.

If the same player's Step-5 decision state appears again, the coordinator throws `InvalidGameStateException` rather than trusting the strategy/effect combination to eventually finish. A separate hard ceiling of **100 Step-5 decisions per player** protects against malformed loops that keep producing new observable states forever.

Focused tests prove both protections:

- a self-replenishing Wisp that restores the exact same state is rejected once other legitimate progress has settled;
- an intentionally malformed Wisp loop that changes VP every time avoids repeated-state detection but is stopped by the hard ceiling.

B3 does not change Human Baseline tactical choice behavior. It only makes the Battle caller safe before later Support-heavy strategy work.

B3 verification:

```text
compileKotlin + compileTestKotlin
    BUILD SUCCESSFUL

focused:
    BattleActionCoordinatorTest

    8 tests
    0 failures
    0 errors
    0 skipped
```

## B4 — `BattleRowAssessor`

**COMPLETE.**

`BattleRowAssessor` is now the shared single-purpose (`operator fun invoke`) source of multiplayer-aware row facts. It reports:

- row availability and participating players;
- actual high players / winner IDs using the same shared-winner versus all-player-tie semantics as `StrikeResolver`;
- Score Benchmark player IDs, total, and margin across all participating opponents;
- Live Threat player IDs, total, and margin across participating opponents who are not Done;
- actual Wound risk;
- `allOpponentsDone`;
- temporary `securedForNow`;
- temporary `potentiallyHopeless`.

Done players remain in Score Benchmark/Strike reality but are removed only from Live Threat. Withdrawn opponents are removed from Strike participation entirely. Closed rows and actor-withdrawn rows return unavailable assessments rather than being treated as tactical candidates.

The secured and hopeless thresholds are read through the injected `HumanBaselinePolicy`; they are observations recalculated from the current context, never persistent row memory. The assessor also accepts an optional projected `BattleView` so later hypothetical-action checkpoints can reuse the same row semantics without consuming or mutating live game state.

Focused tests cover shared winners, all-player no-winner ties, withdrawal, closed/actor-withdrawn rows, Wound risk, Score Benchmark versus Live Threat, tied benchmark/threat groups, all-Done fixed benchmarks, secured rows, Done-nearest-opponent semantics, potentially hopeless rows, and policy overrides.

B4 verification:

```text
compileKotlin
    BUILD SUCCESSFUL

compileTestKotlin
    BUILD SUCCESSFUL

focused:
    BattleRowAssessorTest

    15 tests
    0 failures
    0 errors
    0 skipped
```

## B5 — `BattleSwingEvaluator`

**COMPLETE.**

`BattleSwingEvaluator` now implements the approved shared tactical transition model:

```text
WIN_FLIPPED       tier 5
TIE_ACHIEVED      tier 4
WOUND_PREVENTED   tier 3
WOUND_CREATED     tier 2
SECURED_CREATED   tier 1
NONE              tier 0
```

The tier is multiplied by `HumanBaselinePolicy.battleTransitionScale(context)`, so the default scale of 100 yields bases 500/400/300/200/100. Only the strongest positive transition contributes a base for one row. Ordinary Score-Benchmark margin movement provides the raw detail; `SECURED_CREATED` instead uses Live-Threat margin movement because secured status is defined against opponents who can still respond.

Harmful movement is scored as the negative of what reversing the same movement would score. This gives swaps and collateral damage a symmetric cost without a second adverse-transition table. Changes that cross no named boundary receive only their signed raw margin movement.

The evaluator accepts `BattleRowAssessment` directly and also exposes `BattleSwingState` with `Double` margins so later expected-value analysis can preserve values such as D8 = 4.5 without early rounding. `BattleActionSwing` sums one contribution per affected row with no default multi-row cap.

B5 remains a factual tactical layer only: it does not decide resource conservation, continuation, tempo, or whether a row deserves attention. Those remain higher-level Battle decisions.

B5 verification:

```text
compileKotlin
    BUILD SUCCESSFUL

compileTestKotlin
    BUILD SUCCESSFUL

focused:
    BattleSwingEvaluatorTest
    BattleRowAssessorTest
    HumanBaselinePolicyTest

    40 tests
    0 failures
    0 errors
    0 skipped
```

## B6 — `BattleVpImpact` + `BattleActionAnalyzer` — COMPLETE

B6 adds the separate Strike-VP impact and whole-realization analysis seams needed by later Main and Support strategy.

`BattleVpImpact` mirrors immediate `StrikeResolver` scoring for the actor:

- a non-winner earns 0 VP;
- a winner earns the base 2 VP;
- each participating opponent trailing the actor by at least 5 adds 1 VP;
- shared winners still receive wound VP from lower non-winners;
- expected `Double` totals retain the real five-point wound boundary.

`BattleActionAnalyzer` accepts a complete legal realization and combines:

- `BattleActionSwing` from `BattleSwingEvaluator`;
- projected Strike-VP before/after/gain;
- positive `improvementStepCount`;
- the caller's realization/target object;
- an explicit information mode: `DETERMINISTIC`, `EXPECTED`, or `ACTUAL`.

Improvement steps are deliberately separate from Battle Swing. A wounded-loss-to-win realization may count Wound Prevented + equality/tie + Win as three positive steps while Battle Swing still applies only the single strongest `WIN_FLIPPED` transition base. This metric exists for later premium-resource gates such as Water Refresh.

The analyzer itself never consumes mechanical RNG and does not decide resource conservation, continuation, tempo, or intrinsic card value. Those remain later strategy layers.

## B7 — First Main

**COMPLETE.**

Step-4 First Main now has a dedicated `BattleFirstMainPriority`. Existing Plant and Round-Effect scores remain the intrinsic baseline value, while Draw adds shared tactical analysis using the expected value of the next legally drawn die and its best current legal expected placement. The expected analysis uses `BattleActionAnalyzer`/`BattleSwingEvaluator`, preserves half-point die expectations, and never consumes mechanical RNG.

The expected best row justifies whether Draw is attractive; it does **not** pre-commit the actual rolled die to that row. B8 routes the existing post-roll `chooseDiePlacement` hook through the same placement analyzer using the actual value and fresh context.

Target-dependent Plant/Effect projection is intentionally deferred to B15. The First-Main request does not contain those downstream target choices, so B7 does not duplicate Effect legality/target logic merely to manufacture a top-level projection. Existing intrinsic/contextual Plant scores therefore remain active until action/target alignment is implemented against the real Effect requests.

## B8 — actual Battle die placement

**COMPLETE.**

`BattlePlacementPriority` now projects each row supplied by the current `ChooseBattleDiePlacementRequest` through `BattleDiePlacementAnalyzer` in `ACTUAL` mode. The known rolled value—not the die's expectation—is scored with current Battle Swing. Because actual die values, row totals, and policy scales are integral, the tactical result remains an exact integral `PriorityScore`; exact top-score ties therefore continue through `BaselineScoreEngine` to `StrategyRandomizer` without rounding-induced ties.

Every normal Draw, Mulch, and Effect placement path already creates its decision context after the roll and immediately before the placement request. The analyzer also enforces current closed/withdrawn/full-square legality, while the request limits comparison to the engine's current legal rows.

Rule-mandated replacements continue to use `placeNewHandDieInRow`, which places directly and never invokes `chooseDiePlacement`.

B8 focused verification covered the Battle analysis/strategy package plus the resolver, Main Draw, Battle Mulch, Draw-effect, and Petal To Die 4 placement paths:

```text
95 tests
0 failures
0 errors
0 skipped
```

Battle remains uncertified pending B9–B17 and Stage C.

## B9 — Support capacity

**COMPLETE.**

`BattleSupportCapacityAssessor` returns normalized counts for Wisps, Water, usable Mulch, Worms, Bees, and face-up usable Butterflies. Each physical resource instance contributes at most one remaining Support move even when it produces several legal targets or action forms:

- Water reroll and Refresh choices share the same Water pool;
- Worm placement and Plant Flip choices share the same Worm pool;
- Bee row targets share the same Bee pool;
- each face-up Butterfly counts once across all target dice;
- each playable own Wisp and usable Mulch token counts once.

Own capacity uses the current legal Step-5 choices, preserving exact effect and target legality. Public opponent capacity uses only visible resources and Battle state. Hidden opponent Wisp identities are never inferred; each held Wisp counts as one potential move. Done opponents have zero remaining capacity.

The assessment accepts the relevant Live-Threat opponent IDs from its caller, validates that they identify active opponents, and exposes both their individual capacities and the strongest capacity. It deliberately does not add opponents together. Determining close contested rows and which Live Threats are relevant remains outside B9.

B9 focused verification covered the five direct capacity-contract tests plus the shared Battle analysis/strategy package, Decision Context creation, and the Battle action coordinator:

```text
70 tests
0 failures
0 errors
0 skipped
```

Battle remains uncertified pending B10–B17 and Stage C.

## B10 — Support reachability + continuation

**COMPLETE.**

`BattleSupportReachability` makes the deliberately limited optimistic aggregate
judgment permitted by the Battle contract. For every available non-secured row it
combines all currently legal Bees at current Bee value with expected keep-better
Butterfly improvement. Multiple Butterfly moves may contribute, but their aggregate
cannot exceed the visible dice's physical headroom. No mechanical RNG is consumed.

Worm, Water, Mulch, and Wisp capacity is not added to generic row power. Those
resources remain available for the dedicated direct/enabling rules in B11/B12.

Each row result retains its deterministic/expected `BattleActionAnalysis`, projected
Strike-VP gain, and policy-derived meaningful threshold. The default remains +2 VP,
while injected policy overrides flow through the same seam.

`BattleContinuationAssessor` rebuilds available row facts, normalized own capacity,
and cumulative reachability on every invocation. Its result continues when:

```text
individually worthwhile Support
OR
cumulative path with meaningful Strike-VP gain
```

It reports Final Main when no relevant rows exist, every relevant row is secured,
no Support moves remain, or no worthwhile path remains. The individually-worthwhile
input is intentionally supplied by B11/B12; B14 will turn this factual assessment
into the actual Step-5 choice.

B10 focused verification covers the eleven direct reachability/continuation tests
plus the shared Battle analysis/strategy package, Decision Context creation, and the
Battle action coordinator:

```text
81 tests
0 failures
0 errors
0 skipped
```

Battle remains uncertified pending B11–B17 and Stage C.

## B11 — direct Support analysis

**COMPLETE.**

`BattleDirectSupportAnalyzer` builds deterministic or expected realizations for
ordinary direct Support candidates and evaluates them through
`BattleActionAnalyzer`:

- Bee and direct Worm placement use current round-modified Critter value on the
  exact target row;
- Butterfly uses expected keep-better gain on the exact visible die and row;
- Water reroll uses signed expected reroll gain on the exact visible die and
  row;
- Mulch uses the stored die's expected roll and the highest-Battle-Swing legal
  placement.

Water is categorically disqualified unless its expectation creates a positive
named Battle transition. Mulch is categorically disqualified unless its
expected best placement creates `WIN_FLIPPED`. Direct results expose
`individuallyWorthwhile` for later B10/B14 orchestration. No hypothetical
analysis consumes mechanical RNG or pre-commits actual post-roll placement.

Direct Support priority now comes from shared Battle Swing, without applying
pre-Battle Critter reserves. Card-local influences remain valid current-state
synergies. Ordinary Wisps preserve card-specific intrinsic value and normal
unplayed-Wisp VP opportunity cost. The current tactical Wisp actions are the
special B13 cases: cross-player die swap, immediate Strike resolution, and
two-step die-upgrade target selection. Worm commitment/enabling rules remain
B12, and B14 still owns Support-versus-Final-Main orchestration.

B11 focused verification:

```text
147 tests
0 failures
0 errors
0 skipped
```

Full regression:

```text
unit:        1,074 passed
integration:    72 passed
simulation:     10 passed
```

Battle remains uncertified pending B12–B17 and Stage C.

## B12 — enabling Support analysis

Implement Worm Flip and Water Refresh.

Worm is primarily refresh Support. Direct Worm +1 placement normally requires projected Strike-VP gain of at least 2.

Water Refresh requires at least two improvement steps.

Prefer Worm to Water for an equivalent single-Plant recovery when appropriate.

## B13 — special Battle effect evaluators

Add dedicated focused helpers for:

1. cross-player same-size die swap Wisp — normally projected VP gain >= 2;
2. immediate Strike-resolution Wisp — lead every opponent by at least 5 and each relevant active contender has at least 3 remaining Support moves;
3. two-step die-upgrade target selection — maximize legal resulting die size, then use expected Battle context for ties.

Do not bury these special policies in one giant Effect strategy branch.

## B14 — Support vs Final Main orchestration

Replace the current flat Support-vs-Final-Main scoring and fixed finishing bonus.

Use continuation assessment:

```text
worthwhile continuation
    -> choose best worthwhile Support and remain active

no worthwhile continuation
    -> choose current best Final Main, resolve fully, become Done
```

Tempo is a modifier for useful soft Support, not the primary continuation gate.

## B15 — action / target / branch alignment

Audit affected Battle Effect decisions so the tactical reason that selects an action also drives downstream target/branch choice.

Cover own/opponent dice, swaps, Strike Rows, Plants, Wisps, random-result timing, and relevant card branches.

This may improve Effect code but does not certify the overall Effect Choices area.

## B16 — single-purpose helper API cleanup + behavior-contract tests

For newly added or materially touched focused one-operation helpers, prefer `operator fun invoke()`.

Do not perform unrelated repository-wide style churn.

Restructure direct Battle tests with a clearly identifiable `Human Baseline Behavior Contract` section. Keep wiring/helper/edge-case/engine tests separate.

## B17 — focused compile / test / fix

Compile production and test Kotlin, then run the focused Battle, analysis-helper, policy, coordinator, placement, affected Effect, and relevant integration tests.

Fix focused failures until green. Record exact test totals/results.

Do not mark Battle certified yet.

## Stage C

### C1 — durable certification documentation

Update Battle docs, `HUMAN_BASELINE.md`, local README/KDoc, testing references, and certification wording. Keep Battle pending until C2 succeeds.

### C2 — full regression

Run:

```bash
GRADLE_USER_HOME=/mnt/data/leaf-gradle-portable \
    ./gradlew --offline --no-daemon \
    test integrationTest simulationCheck
```

Verify relative Markdown links.

### C3 — certify

Only after C2 succeeds:

- mark Battle CERTIFIED;
- update progress to 3 of 8 major areas and 6 of 30 hooks, if the 3-hook count remains current;
- package the final leaf-rooted patch.
