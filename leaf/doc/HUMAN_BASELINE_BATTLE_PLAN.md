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
- [ ] B6 — `BattleVpImpact` + `BattleActionAnalyzer`
- [ ] B7 — First Main
- [ ] B8 — actual Battle die placement
- [ ] B9 — Support capacity
- [ ] B10 — Support reachability + continuation
- [ ] B11 — direct Support analysis
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

## B6 — `BattleVpImpact` + `BattleActionAnalyzer`

Add the separate simple Strike-VP impact metric needed for substantial Support commitment and special-effect gates.

Add shared action analysis for deterministic, expected-random, and actual-known results, including:

- per-row Battle Swing;
- multi-row collateral;
- improvement-step count;
- target realization.

Hypothetical analysis must never consume mechanical RNG.

## B7 — First Main

Refactor First Main to combine intrinsic action/card value with shared tactical analysis.

Implement Plant-vs-Draw, expected Draw placement, Round Effect tactical contribution, random-information timing, and no long-horizon conservation.

## B8 — actual Battle die placement

Refactor placement to use actual known die value plus fresh Battle state and shared Battle Swing.

Respect forced placement and exact tactical ties via `StrategyRandomizer`.

## B9 — Support capacity

Count Support move resources rather than legal target multiplicity.

Support own and public-opponent capacity, relevant Live-Threat opponents, and hidden-Wisp information boundaries.

## B10 — Support reachability + continuation

Implement limited cumulative reachability primarily from Bees and expected Butterfly improvement.

Implement the continuation gate:

```text
individually worthwhile Support
OR
cumulative path with meaningful Strike-VP gain
```

If every row is secured or no worthwhile path remains, commit Final Main.

## B11 — direct Support analysis

Migrate direct Supports to shared tactical analysis:

- Bee;
- Butterfly;
- Water reroll;
- Mulch;
- ordinary direct Wisp effects;
- other direct Support discovered in current source.

Apply premium-resource gates.

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
