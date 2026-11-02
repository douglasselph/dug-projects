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

Stage B is complete through B17. Stage C is complete. Battle is **CERTIFIED**.

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
- [x] B12 — enabling Support analysis
- [x] B13 — special Battle effect evaluators
- [x] B14 — Support vs Final Main orchestration
- [x] B15 — action / target / branch alignment — COMPLETE
  - [x] B15A — inventory + alignment map
  - [x] B15B — own-die target alignment (B15B1a/B15B1b/B15B2/B15B3/B15B4)
  - [x] B15C — opponent / any-Battle-die target alignment
  - [x] B15D — Strike-Row + multi-row alignment
  - [x] B15E — Plant / player / Wisp / branch alignment (B15E1/B15E2/B15E3)
  - [x] B15F — random-timing + B13 end-to-end alignment (B15F1/B15F2)
  - [x] B15G — representative real-engine integration + documentation + focused B15 verification
- [x] B16 — helper/API + behavior-contract cleanup
  - [x] B16-1 — focused helper API audit/cleanup (no production change required)
  - [x] B16-2 — Human Baseline Battle behavior-contract test organization/coverage
- [x] B17 — focused compile / test / fix

Stage C:

- [x] C1 — durable certification documentation
- [x] C2 — full `test integrationTest simulationCheck` regression + Markdown-link verification
- [x] C3 — mark Battle CERTIFIED and package final patch

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

B11 routes ordinary direct Supports through shared whole-action analysis. Bee and direct Worm placement use current effective values; Butterfly and Water reroll use expectation on the exact die row; Mulch uses expected roll plus best legal placement. Premium-resource gates reject Water without a positive named transition and Mulch without expected `WIN_FLIPPED`, while special target-dependent Wisps remain B13.

B12 adds one-step enabling analysis for Worm Flip and Water Refresh. It compares the best immediate Plant Main unlocked by refresh with the best Final Main already available, gates direct Worm placement on meaningful projected Strike-VP gain, requires Water's enabled Main to cross the policy minimum improvement steps, treats refreshed Butterflies only as a secondary bonus, and prefers Worm for equivalent single-Plant recovery.

B13 adds the three approved dedicated special Battle effect evaluators and routes their exact target decisions through shared tactical reasoning.

B14 replaces the flat Support-versus-Final-Main score race with explicit continuation orchestration. `BattleTurnOrchestrator` asks the fresh `BattleContinuationAssessor` first, admits only individually worthwhile Supports or ordinary Bee/Butterfly moves that advance a meaningful cumulative path, and otherwise restricts the choice to the current legal Final Mains. `BattleTempoAssessor` derives close live contests and compares normalized Support capacity against the strongest relevant Live Threat; its result only adds a small modifier to already-useful soft Wisp Support. Every actual Support returns control to the engine, so the next pass receives a fresh context and reassesses from current state. Final Main no longer receives the old arbitrary finishing bonus; authoritative Done marking remains the coordinator's post-resolution responsibility.

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

`BattleDirectSupportAnalyzer` now migrates ordinary direct Supports to shared
deterministic/expected tactical analysis:

- Bee and direct Worm placement use their current round-modified values;
- Butterfly uses expected keep-better gain on its exact die row;
- Water reroll uses expected reroll gain and requires a positive named transition;
- Mulch uses its expected stored-die roll, best legal placement, and requires
  expected `WIN_FLIPPED`.

Target-dependent and special-policy Wisps remain intentionally deferred to B13.
Hypothetical analysis does not mutate game state or consume mechanical RNG.

B11 full verification:

```text
unit:        1,074 tests, 0 failures, 0 errors, 0 skipped
integration:    72 tests, 0 failures, 0 errors, 0 skipped
simulation:     10 tests, 0 failures, 0 errors, 0 skipped
```

## B12 — enabling Support analysis

**COMPLETE.**

`BattleEnablingSupportAnalyzer` implements the bounded one-step sequence:

```text
Support -> refreshed Plant -> best visible immediate Final Main
```

For Worm Flip, the target must be face down and the unlocked Plant opportunity
must improve on the best currently available Final Main. Generic Plant
preservation is not used as a substitute for that incremental value, and a
face-up Plant is never flipped merely to delay Final Main.

For Water Refresh, at least one face-down Plant must exist and its best unlocked
Main must both improve the visible Final Main and meet
`battleWaterRefreshMinImprovementSteps` (default 2). Refreshed Butterflies add a
secondary future-Support bonus only after that Plant gate passes; they cannot
justify Water by themselves.

`BattleEnabledPlantAnalyzer` combines existing Plant intrinsic scoring with the
best deterministic or expected immediate own-die realization currently visible.
It neither executes the effect nor commits a later target. Target/branch alignment
remains B15.

Direct Worm placement now requires the policy's meaningful projected Strike-VP
gain (default 2), keeping placement and Flip as competing uses of the same physical
resource. When Worm and Water offer equivalent recovery of one Plant and no
Butterfly, Worm receives the deterministic resource-conservation preference.

B12 verification:

```text
focused Battle/context: 158 tests, 0 failures, 0 errors, 0 skipped
unit:                  1,085 tests, 0 failures, 0 errors, 0 skipped
integration:              72 tests, 0 failures, 0 errors, 0 skipped
simulation:               10 tests, 0 failures, 0 errors, 0 skipped
```

Battle remains uncertified pending B13–B17 and Stage C.

## B13 — special Battle effect evaluators

**COMPLETE.**

B13 adds three dedicated helpers rather than embedding special-case Battle logic in
one large Effect-strategy branch:

1. `BattlePollenTheftEvaluator` projects every legal same-size cross-player swap as
   one complete deterministic multi-row realization. Actor benefit and collateral
   benefit/harm use `BattleActionAnalyzer`, `BattleSwingEvaluator`, and
   `BattleVpImpact` together. The approved meaningful-VP gate remains policy driven
   through `battleMinimumMeaningfulVpGain` (default 2).
2. `BattleImmediateStrikeResolveEvaluator` applies Wisp's Resolve's lock-in policy
   to each legal row. It requires the policy lead over every participating opponent
   (default 5) and uses authoritative Done state plus normalized public Support
   capacity for each active contender (default minimum 3 moves). Hidden Wisp
   identities are never inferred; only the public count contributes capacity.
3. `BattleTwoStepUpgradeEvaluator` reproduces Overgrowth's two-available-step,
   skip-missing-size ladder from the immutable Graft Bed view. It first maximizes
   the legal resulting die size, then uses expected current-Battle analysis only to
   break equal-result-size choices. It does not roll or pre-resolve the replacement.

`HumanBaselineEffectStrategy` now routes only these exact B13 targets through the
focused evaluators: Pollen Theft's complete swap, Wisp's Resolve's Strike Row, and
Overgrowth's die target. Exact scored ties still use `StrategyRandomizer` through
the existing `BaselineScoreEngine`.

B13 verification:

```text
compileKotlin + compileTestKotlin
    BUILD SUCCESSFUL

focused special evaluators + Effect strategy integration:
    14 tests
    0 failures
    0 errors
    0 skipped

focused Battle / affected Effect mechanics:
    126 tests
    0 failures
    0 errors
    0 skipped

full regression:
    unit:        1,099 tests, 0 failures, 0 errors, 0 skipped
    integration:    72 tests, 0 failures, 0 errors, 0 skipped
    simulation:     10 tests, 0 failures, 0 errors, 0 skipped
```

Battle remains uncertified pending B14–B17 and Stage C.

## B14 — Support vs Final Main orchestration

**COMPLETE.**

`HumanBaselineBattleStrategy.chooseTurnAction()` no longer scores every Support and Final Main in one flat pool and no longer adds a fixed finishing bonus to Final Main.

The Step-5 pipeline is now:

```text
fresh DecisionContext
    -> BattleTurnOrchestrator
    -> direct/enabling Support usefulness
    -> BattleContinuationAssessor

continuation worthwhile
    -> choose among currently worthwhile Supports only
    -> resolve one Support
    -> engine builds a fresh next-pass context
    -> reassess everything

continuation not worthwhile
    -> choose among current legal Final Mains only
    -> resolve fully
    -> coordinator marks Done
```

`BattleTurnOrchestrator` reuses the established B10-B12 helpers rather than duplicating their rules. Premium-resource gates remain authoritative. If continuation exists only because ordinary cumulative reachability succeeds, only legal Bees and Butterflies that can advance that B10 path are admitted as Support candidates.

`BattleTempoAssessor` implements the bounded acting-last modifier from A6. It identifies close live contests using `battleCloseMargin`, uses Live Threat rather than a Done Score Benchmark, and compares own normalized Support capacity with the strongest relevant opponent rather than a coalition. Tempo never admits a rejected Support; it only adds a small preference to an already-positive soft Wisp candidate.

Focused unit behavior verifies secured-state Final Main, individually worthwhile Support, meaningful cumulative continuation, no-worthwhile-path Final Main, close-contest tempo, and Done-opponent tempo exclusion. Focused integration exercises the real Battle coordinator for both:

```text
worthwhile Support -> fresh reassessment -> Final Main -> Done
```

and:

```text
legal Support exists but fails its gate -> immediate Final Main -> Done
```

B14 verification:

```text
focused Battle unit:
    108 tests
    0 failures
    0 errors
    0 skipped

focused Battle action integration:
    5 tests
    0 failures
    0 errors
    0 skipped

full regression:
    unit:        1,105 tests, 0 failures, 0 errors, 0 skipped
    integration:    74 tests, 0 failures, 0 errors, 0 skipped
    simulation:     10 tests, 0 failures, 0 errors, 0 skipped
```

Battle remains uncertified pending B15-B17 and Stage C.

## B15 — action / target / branch alignment

### Overall purpose

B15 closes the gap between top-level Battle action valuation and the later Effect target/branch request. The intended contract is:

```text
enumerate legal realizations
    -> evaluate them with compatible shared Battle reasoning
    -> use the best realization when valuing whether to take the action
    -> when the downstream legal choice is requested, use the same reasoning
    -> normally select the realization that justified taking the action
```

Alignment means **compatible current-state reasoning**, not a hidden stale target commitment. If legitimate game state changes before the downstream decision, Human Baseline must rebuild from the fresh `DecisionContext` and re-evaluate the then-legal choices.

B15 may improve `HumanBaselineEffectStrategy`, but it does **not** certify the overall Effect Choices decision area.

### B15A — inventory + alignment map — COMPLETE

B15A inspected the current source rather than relying on historical hook names. The production `EffectStrategy` currently declares 20 methods. The phase-aware `GameEffectDecisionRequirements` contract exposes **17 of those methods during Battle**, producing **50 Battle effect/mechanism pairings**. Three EffectStrategy methods are not current Battle B15 seams:

- `chooseBattleDie` — the shared helper exists, but no current `GameEffectDecisionRequirements` entry uses `EFFECT_BATTLE_DIE`, and `chooseRequiredBattleDie(...)` has no production caller;
- `chooseDice` — `EFFECT_DICE_SET` is currently Cultivation-only for its one effect;
- `chooseOptionalPlant` — current optional-Plant requests are Cultivation-side branches; their Battle variants use Wound/other decision families instead.

The 50 current Battle pairings collapse into **22 implementation/audit slices** because several effects intentionally share one strategy request shape while some shared hooks have materially different Battle behavior.

Current top-level alignment facts found by B15A:

- Step-4 Plant and Round-effect actions still receive intrinsic/contextual scores in `BattleFirstMainPriority`; target-dependent tactical projection was deliberately deferred from B7 to B15.
- Step-5 Wisp willingness still begins with the card-local `wispPlayScore(...)`; B14 decides whether a Wisp is an individually worthwhile soft Support but does not generally make the Wisp's downstream target analyzer authoritative at top level.
- Generic `chooseDie(...)`, `chooseRootWellBattle(...)`, `scorePair(...)`, and non-special `chooseStrikeRow(...)` still use local die arithmetic / `RowNeed`-style heuristics rather than complete shared `BattleActionAnalyzer` realizations.
- The B13 downstream target hooks are already specialized: Overgrowth uses `BattleTwoStepUpgradeEvaluator`, Pollen Theft uses `BattlePollenTheftEvaluator`, and immediate Strike resolution uses `BattleImmediateStrikeResolveEvaluator`. B15F2 verified their **top-level willingness + downstream target** paths end to end without redesigning their target rules.
- `chooseOptionalDie(...)` for Wispquake uses expected protection value before the reroll. B15F1 completed the broader random-information audit and confirmed/repaired the target-before/post-random boundaries across B15.

No broad production behavior was changed in B15A.

#### B15A current alignment inventory

| Primary owner | Strategy hook slice | Current Battle effects / decisions | Current downstream reasoning | B15 disposition |
| --- | --- | --- | --- | --- |
| B15B1a | generic `chooseDie` — pure deterministic own-die value transforms | `DOUBLE_ONE_DIE`; `FLIP_OWN_DIE_TO_OPPOSITE_FACE`; `RAISE_ANY_DIE_PLUS_1`; `RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER`; `RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE`; `RAISE_DIE_PLUS_3`; `RAISE_DIE_PLUS_4`; `SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE`; `SET_DIE_UP_TO_D12_TO_MAX`; `SET_LOWEST_VALUE_DIE_TO_MAX` | generic target-value arithmetic in `CardScoringHelpers.scoreDieTarget(...)` | align exact affected row with shared tactical analysis |
| B15B1b | generic `chooseDie` — deterministic target with secondary/collateral Battle consequence | `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW`; `RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE`; `RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW`; `RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE` | local raise gain plus broad intrinsic/RowNeed adjustments | analyze the complete immediate realization; leave separate row branch to B15D |
| B15B2 | generic `chooseDie` — discard/draw/Mulch/return source target | `DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE`; `DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE`; `DISCARD_ONE_DIE_DRAW_TWO`; `GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD`; `MULCH_DIE_FROM_DISCARD`; `MULCH_DIE_FROM_HAND`; `ROLL_DIE_FROM_DISCARD_INTO_HAND` | generic/local source-die heuristics | align source choice with expected immediate Battle consequence where legal; B15F1 verifies later RNG/placement timing |
| B15B3 | generic `chooseDie` — reroll/upgrade own-die target | `DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE`; `REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS`; `UPGRADE_DIE_AND_USE_NOW`; `UPGRADE_DIE_FROM_HAND`; `REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW` | expected/local die gain; some later placement/row consequence | use expectation before RNG and shared row realization; B15F1 audits timing |
| B15B4 | compound own-die target | `chooseDiePair` for `SET_DIE_TO_MATCH_ANOTHER`; `chooseCritterAndDie` for `TRASH_CRITTER_TO_RAISE_DIE_PLUS_5` | local numeric gain / reserve arithmetic | align pair/combined target with current Battle realization without adding future-Battle planning |
| B15C | `chooseRootWellBattle` | `GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE` | expected reroll arithmetic plus RowNeed/value | compare complete own-two versus opponent-one expected Battle outcomes; preserve target-before-RNG timing |
| B15D | own-die pair / Strike-Row realizations | `chooseOptionalDiePair` for `DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE`; `chooseDiePair` for `DRAW_ONE_DIE_AND_SWAP_TWO_OWN_DICE_RAISE_ONE_PLUS_2_IN_BATTLE`; non-special `chooseStrikeRow` for `RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE`, `REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW`, and `SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3` | pair-local RowNeed arithmetic; generic row-need bonus | evaluate complete row/multi-row consequences and remove enum/first-legal bias |
| B15E1 | Plant/opponent-Plant effect target | `chooseOpponentPlantWound` for 3 Battle effects; `choosePlantEffect` for `REUSE_SPENT_ROOT_OR_VINE_EFFECT` | card loss/play values | align immediate Battle-enabled/disabling value without deep future-combo search |
| B15E2 | resource / player / Wisp-set target | `chooseBeeSource`; `chooseButterflyTarget` (2 effects); `chooseWispsToKeep`; `chooseDieSize`; `choosePlayer` | local resource/card heuristics | audit top-level compatibility; preserve simple logic when Battle consequences do not materially distinguish targets |
| B15E3 | qualitative card branch | `choosePetalToDie4`; `chooseOEdelweiss` | existing branch/card scorer logic | align Battle branch choice with top-level action value; B15F1 audits any later random/placement seam |
| B15F1 | random-timing primary seam | `chooseOptionalDie` for `REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN` | expected protected-value calculation before reroll | verify target-before-RNG commitment and audit all other B15 random seams cross-cutting B15B–B15E |
| B15F2 | B13 end-to-end special alignment | Overgrowth `chooseDie`; Pollen Theft `chooseCrossPlayerDieSwap`; immediate-resolve `chooseStrikeRow` | dedicated B13 evaluators | preserve target policy; verify the top-level decision that spends/plays the effect is compatible with the dedicated target evaluator |

#### B15A counts

The 22 primary slices above account for all 50 Battle effect/mechanism pairings:

- **B15B:** 5 slices / 28 effect-mechanism pairings;
- **B15C:** 1 slice / 1 pairing;
- **B15D:** 3 slices / 5 pairings;
- **B15E:** 9 slices / 12 pairings;
- **B15F:** 4 slices / 4 pairings.

`B15G` is the integration/documentation/verification closeout and owns no new effect-mechanism pairing.

#### Timeout-safe refinement discovered by B15A

The source inventory confirms that the single B15B checkpoint from the external timeout-safe handoff is still too broad: generic `chooseDie(...)` alone serves 27 Battle effect pairings, 26 of them outside the B13 Overgrowth special case. Therefore B15B is subdivided before implementation:

- **B15B1a — pure deterministic own-die transforms** (10 pairings);
- **B15B1b — deterministic own-die targets with secondary/collateral Battle consequences** (4 pairings);
- **B15B2 — discard/draw/Mulch/return source-die alignment** (7 pairings);
- **B15B3 — reroll/upgrade own-die alignment** (5 pairings);
- **B15B4 — compound pair/Critter + die alignment** (2 pairings).

B15E is also split because it spans several unrelated request families:

- **B15E1 — Plant/opponent-Plant alignment** (4 pairings);
- **B15E2 — resource/player/Wisp-set target alignment** (6 pairings);
- **B15E3 — qualitative branch alignment** (2 pairings).

B15F is split into:

- **B15F1 — random-information timing audit**, including the Wispquake keep-one seam and the target-before/post-random boundaries that cross B15B–B15E;
- **B15F2 — B13 end-to-end special alignment**, covering Overgrowth, Pollen Theft, and immediate Strike resolution without redesigning their B13 target policies.

This refinement is deliberately more granular than the external B15A–B15G sketch because the current source proves that `chooseDie(...)` is a much larger shared seam than the earlier plan could know before inventory.

### B15B1a progress — pure deterministic own-die transforms

B15B1a was implemented in timeout-safe micro-slices rather than as one ten-effect patch. All five slices are complete:

- [x] **fixed Plant die raises** — `RAISE_ANY_DIE_PLUS_1` and `RAISE_DIE_PLUS_4` choose their downstream Battle die target by the exact affected row's shared deterministic Battle analysis rather than generic numeric raise gain. A capped smaller raise that flips a Strike can therefore beat a larger raw raise on a strategically weaker row. Exact tactical target ties continue to use `StrategyRandomizer`.
- [x] **remaining straightforward raises** — `RAISE_DIE_PLUS_3`, `RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER`, and `RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE` use the same affected-row tactical projection. The creature-count effects derive their exact raise amount from the current Creature composition before analyzing each legal die.
- [x] **Double and opposite-face Flip** — `DOUBLE_ONE_DIE` and `FLIP_OWN_DIE_TO_OPPOSITE_FACE` now project their exact deterministic signed value change through the affected Strike row. A smaller raw Double/Flip improvement that changes the Strike outcome can therefore beat a much larger numeric improvement on a row that remains strategically worse. Exact tactical ties still use `StrategyRandomizer`.
- [x] **set-to-maximum effects** — `SET_DIE_UP_TO_D12_TO_MAX` and `SET_LOWEST_VALUE_DIE_TO_MAX` project the exact gain to the selected die's physical maximum through its affected Strike row. A smaller raw set-to-max gain that flips a Strike can therefore beat a larger raw gain on a row that remains a loss. `SET_LOWEST_VALUE_DIE_TO_MAX` continues to choose only among the engine-provided tied-low legal dice.
- [x] **set-to-1 plus VP** — `SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE` projects the exact negative value change from the selected die to 1 through its affected Strike row. The executor awards VP after the selected legal die becomes a 1, so every legal target produces the same number of showing-1 dice and therefore the same VP reward. No new VP-versus-Battle policy is needed for downstream target choice: the target-dependent decision is simply which legal die can be reduced to 1 with the least harmful immediate Battle consequence.

These completed slices change only downstream Battle target alignment for straightforward deterministic own-die transforms. B15B1a is now complete. Cultivation targeting is unchanged, and this does not certify Effect Choices.

### B15B1b progress — deterministic own-die targets with secondary/collateral Battle consequences

B15B1b is also being implemented in timeout-safe micro-slices.

- [x] **opposing-row collateral** — `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW` and `RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW` now use shared complete-realization analysis for both top-level enabled-Plant valuation and downstream Battle die targeting. `BattleOwnDieCollateralAnalyzer` mirrors the actual engine order: Sapping Snapdragon applies the capped +2, reduces each opposing die by its actual capped amount, then raises the chosen die by the total reduction; Bloom Backflip applies the capped +1 and then flips only opposing D6+ dice still higher than the raised die. Opponent changes are included in the projected Strike totals, including harmful flips that can improve an opponent.
- [x] **non-row secondary consequences** — `RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE` and `RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE` now evaluate the selected +1 through its actual Strike row. Bursting Blossom additionally gives the selected target its existing card-scorer value for *creating* one additional maximum die when a die is actually drawable; existing maximum-die Draws are target-invariant. Root & Scoot deliberately does not pre-select its later withdrawal row: that mandatory row decision remains a fresh downstream request owned by B15D.

B15B1b is complete. These slices do not implement the later Strike-Row branch decisions and do not certify Effect Choices.

### B15B2 progress — discard/draw/Mulch/return source-die alignment

B15B2 is being implemented as smaller source-target slices.

- [x] **discard source for Draw effects** — the source-die choice for `DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE`, `DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE`, and `DISCARD_ONE_DIE_DRAW_TWO` now starts from the selected die's actual Battle row instead of generic weakest-die arithmetic. The strategy mirrors normal Draw order using visible die sizes and mathematical expected rolls only. Forced same-row replacements are included in the expected row projection; later optional swaps and free post-roll placements remain fresh downstream decisions and are not pre-committed. The free-placement two-Draw effect values the immediate tactical cost of removing the source die and only uses source-dependent expected Draw value as a small non-positional adjustment. No mechanical RNG is consumed.
- [x] **Mulch/discard/return source choices** — audited `GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD`, `MULCH_DIE_FROM_DISCARD`, `MULCH_DIE_FROM_HAND`, and `ROLL_DIE_FROM_DISCARD_INTO_HAND`. The three Mulch-source decisions remain on their existing simple behavior because a Mulch created by one of these effects is `PENDING_MULCH` and is not normalized into a usable Mulch token until Battle Cleanup; selecting among those legal sources therefore does not create a distinct immediate current-Battle realization to optimize in this checkpoint. `ROLL_DIE_FROM_DISCARD_INTO_HAND` is different: the selected Discard die is immediately rolled and then placed in Battle, so Human Baseline now compares legal source dice by mathematical expected roll and best currently legal expected Battle placement. The source choice consumes no mechanical RNG and does not pre-commit the later actual placement row. Equal-size source dice remain exact tactical ties regardless of their current Discard face and continue to use `StrategyRandomizer`.

B15B2 is complete and does not certify Effect Choices.

### B15B3 progress — reroll/upgrade own-die alignment

B15B3 is being implemented in bounded random-target slices.

- [x] **simple reroll targets** — `REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS` and the Battle one-die branch of `DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE` now choose the live die by the honest expected change on its actual Strike row rather than by generic expected face gain alone. Root on a Roll uses the exact accepted-face expectation for repeated rerolls until 3+; Root Recall uses ordinary one-roll expectation. Both use `BattleAnalysisMode.EXPECTED`, consume no mechanical RNG, and leave the live die in its existing Battle row. `BattleEnabledPlantAnalyzer` uses the same Root-on-a-Roll expectation so top-level enabled-Plant valuation and downstream target selection remain compatible. Exact tactical ties still use `StrategyRandomizer`.
- [x] **one-step upgrade targets** — `UPGRADE_DIE_AND_USE_NOW` (Root Awakening) now chooses among legal Battle dice by the mathematical expected value of the exact normal next-size replacement on that die's current Strike row. The normal ladder remains D4→D6→D8→D10→D12→D20 with no skip-missing behavior; the effect handler/request still owns Graft Bed legality. No replacement is rolled during strategy analysis, and the selected die's row is not changed or pre-resolved. `BattleEnabledPlantAnalyzer` uses the same expected one-step projection and checks visible next-size Graft Bed availability, keeping enabled-Plant valuation compatible with downstream target selection. `UPGRADE_DIE_FROM_HAND` was audited in this slice but intentionally retains its existing `CompostPriority` behavior: every current concrete source is a Cultivation Round Compost effect, and the effect places the upgraded die in Discard rather than rolling/using it now, so there is no current production Battle action/target path whose row value should be invented here. B13 Overgrowth (`UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW`) remains on its dedicated two-step evaluator unchanged.
- [x] **opposing-collateral reroll target** — `REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW` (Gust of Petals) now values each legal first own-die target as an expected complete immediate Battle realization instead of only the local expected face gain. `BattleGustOfPetalsTargetAnalyzer` applies the selected die's fair expected reroll on its current row, then compares the currently visible open-row branches for the later forced opposing rerolls. In each candidate row, opponent dice above the actor's projected lowest die contribute their fair expected reroll changes before shared `BattleActionAnalyzer` scoring. The strategy uses the best expected later branch only for valuation; it consumes no RNG and does not commit the actual later Strike-Row choice, which remains a fresh post-reroll B15D decision. `BattleEnabledPlantAnalyzer` shares the same helper so top-level enabled-Plant valuation and downstream own-die target choice remain compatible.

B15B3 is complete. The cross-cutting B15F1 audit later confirmed/repaired its random-information timing contract, and B15D completed Gust's actual downstream Strike-Row selection.

### B15 closeout — COMPLETE

The B15A inventory remains the authoritative enumeration of the **50 Battle effect/mechanism pairings**. Every pairing in that table now has a completed disposition. The aggregate accounting is unchanged: B15B = 28 pairings, B15C = 1, B15D = 5, B15E = 12, and B15F = 4, for **50 total**. B15G owns integration/documentation/verification and adds no new pairing.

- [x] **B15B1a** — pure deterministic own-die transforms complete
- [x] **B15B1b** — deterministic own-die targets with secondary/collateral Battle consequences complete
- [x] **B15B2** — discard/draw/Mulch/return source-die alignment complete
- [x] **B15B3** — reroll/upgrade own-die alignment complete
- [x] **B15B4** — compound own-die pair / Critter+die alignment complete
- [x] **B15C** — Root Well own-two versus opponent-one alignment complete
- [x] **B15D** — Strike-Row and swap/multi-row alignment complete
- [x] **B15E1** — Plant/opponent-Plant alignment complete
- [x] **B15E2** — resource/player/Wisp-set target alignment complete. Bee-loved Bloom uses `BattleBeeSourceAnalyzer` to distinguish the immediate public Battle Support danger denied by stealing a Bee from a live opponent; Done opponents receive no denial value. Butterfly target, Wisp-set, die-size, and player choices retain bounded visible-information heuristics where no stronger immediate target-specific Battle realization is required.
- [x] **B15E3-1** — Petal To Die 4 qualitative branch alignment complete. Gain-D4 projects the deterministic D4=4 through best currently legal placement without pre-committing the real later placement; Trash-D4/+4 removes the exact D4 and aggregates the capped +4 changes to all remaining actor Battle dice.
- [x] **B15E3-2** — O Edelweiss qualitative branch alignment complete. Each request values only the current first choice; after resolution the second choice is rebuilt from fresh legal choices and current context. Done remains legal and no two-choice tree is preplanned.
- [x] **B15E3** — qualitative branch alignment complete
- [x] **B15F1** — random-information timing audit/repairs complete. B15 random seams now preserve target-before-RNG commitments, use mathematical expectation before unknown RNG, consume no hypothetical mechanical RNG, and leave post-RNG placement/branch decisions fresh when the rules ask them later. The timing contract is clean for the audited B15 seams, including Wispquake keep-one, rerolls, upgrades, Draw/Forget-Me-Not, Gust, and Petal To Die 4.
- [x] **B15F2-1** — Overgrowth B13 end-to-end alignment complete. Top-level willingness values the same largest-result-family / expected-current-Battle tie-break policy used by the dedicated downstream evaluator.
- [x] **B15F2-2** — Pollen Theft B13 end-to-end alignment complete. Top-level willingness uses the same best complete multi-row realization and meaningful-VP spending gate as `BattlePollenTheftEvaluator`.
- [x] **B15F2-3** — immediate Strike resolution B13 end-to-end alignment complete. Top-level willingness requires a row passing `BattleImmediateStrikeResolveEvaluator`'s established lock-in policy rather than treating any current lead as sufficient.
- [x] **B15F2** — B13 special end-to-end alignment complete
- [x] **B15G-1** — representative real-engine integration complete. Production integration coverage exercises a deterministic Bee target path, Butterfly target-before-RNG/actual-result path, and Pollen Theft B13-special path through legal decision, Human Baseline willingness, downstream choice, real effect execution, resulting Battle state, and Chronicle/observable state.
- [x] **B15G-2** — durable B15 documentation and closeout complete
- [x] **B15G** — complete

B15 and B16 are complete. B17 and Stage C were subsequently completed, and Battle is now **CERTIFIED**.


## B16 — helper/API + behavior-contract cleanup

B16 is split for timeout safety.

### B16-1 — focused helper API audit/cleanup — COMPLETE

The focused Battle helpers introduced or materially touched by B4-B15 were audited for the project's single-operation API convention. The helpers that fundamentally expose one operation already use `operator fun invoke(...)`; helpers retaining named public methods have genuinely distinct operations, evaluation/enumeration surfaces, scoring/tagging surfaces, or companion state-conversion responsibilities. No production API change was justified, so B16-1 is intentionally a no-op rather than style churn. No Battle behavior or engine API was changed.

### B16-2 — Human Baseline Battle behavior-contract tests — COMPLETE

`HumanBaselineBattleStrategyTest` now has a clearly identifiable `Human Baseline Behavior Contract` section. It expresses representative approved behavior through direct strategy choices: First Main tactical choice, actual-value and fresh-context die placement, meaningful versus policy-rejected Support continuation, cumulative Bee reachability, and premium-resource preservation. The section explicitly points to the focused Effect-alignment tests for action/target/branch and target-before-RNG contracts, and to the row/capacity/continuation tests for authoritative Done and Live-Threat semantics rather than duplicating those helper edge matrices. Helper, wiring, engine, and edge-case tests remain conceptually separate. No Battle production behavior was changed.

Focused verification command: `./gradlew --offline --no-daemon test --tests 'dugsolutions.leaf.v35.player.decision.baseline.battle.HumanBaselineBattleStrategyTest'`. In this execution environment Gradle reached `checkKotlinGradlePluginConfigurationErrors`, `processResources NO-SOURCE`, and `processTestResources`, then exceeded the 45-second execution window before Kotlin compilation/test execution completed. Exact observed result: **0 tests reached execution; 0 test failures were observed; no green result is claimed.** B17 remains responsible for the consolidated focused Battle verification.

## B17 — focused compile / test / fix — COMPLETE

B17 verification exposed concrete failures in the representative B15G real-engine integration scenarios. Investigation showed the failures were in integration-test fixtures/expectations rather than production Human Baseline Battle behavior: Butterfly and Pollen Theft expectations did not match the approved target policies, and the Bee fixture did not uniquely establish its intended deterministic winning row. The integration scenarios were corrected without changing production Battle behavior. Focused local reruns then passed, followed by the successful C2 full regression.

## Stage C — COMPLETE

### C1 — durable certification documentation — COMPLETE

The durable Battle contract, implementation plan, overall Human Baseline status, local baseline README, and Battle strategy KDoc were reconciled for certification.

### C2 — full regression — COMPLETE

The full regression command `./gradlew test integrationTest simulationCheck` completed successfully on the final post-B17 source state. Relative Markdown links were also verified for this C3 closeout.

### C3 — certify — COMPLETE

Battle is **CERTIFIED**. The Battle strategy API remains three hooks, so overall progress is now **3 of 8 major Human Baseline areas certified** and **6 of 30 strategy hooks certified**. C3 changes certification/documentation state only; it makes no production Battle behavior change.

### B15G-2 focused verification record

The B15 closeout attempted the focused verification set with the portable offline Gradle environment. Two focused unit-test invocations were attempted: first the Human Baseline Effect-strategy B15 family plus the three B13 end-to-end alignment tests, then a narrowed six-class set covering Bee source, Petal To Die 4, O Edelweiss, Overgrowth, Pollen Theft, and immediate Strike resolution. Both invocations reached Gradle task setup/resource processing but exceeded the execution window before Kotlin compilation/test execution completed. A separate focused `integrationTest` invocation for `B15ActionTargetBranchIntegrationTest` likewise reached integration/test resource processing but exceeded the execution window before test execution.

Exact observed result for this closeout environment: **0 focused tests reached test execution; 0 test failures were reported; no green test result is claimed.** No full repository regression was run. That B15G-2 timeout was historical; B17 and C2 were subsequently completed successfully before C3 certification.
