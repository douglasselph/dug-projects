# Human Baseline Battle Behavior Contract

This document is the durable designer-approved target for **Human Baseline Battle** in Milestone 2.

Implementation lives primarily under:

```text
src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/battle/
```

The incremental implementation sequence is tracked in [`HUMAN_BASELINE_BATTLE_PLAN.md`](HUMAN_BASELINE_BATTLE_PLAN.md). Cross-cutting tuning knobs live in [`HUMAN_BASELINE_POLICY.md`](HUMAN_BASELINE_POLICY.md).

## Status

Battle Stage A (A1–A6) is complete. The current source was inventoried before this contract was approved.

B1 established documentation/shared-policy wiring. B2 moved Done into authoritative Battle-round state and immutable decision context. B3 added repeated-state detection and a hard Step-5 decision ceiling so malformed Support loops cannot run forever. B4 implemented the shared multiplayer-aware `BattleRowAssessor`, including Score Benchmark, Live Threat, actual winner/Wound facts, and temporary secured/hopeless assessments. B5 implemented `BattleSwingEvaluator`, including the policy-scaled 500/400/300/200/100 transition hierarchy, raw swing, symmetric collateral harm, and multi-row aggregation. B6 adds `BattleVpImpact` plus `BattleActionAnalyzer`, so a complete deterministic/expected/actual realization can report Battle Swing, immediate Strike-VP change, collateral effects, and the separate positive-boundary improvement-step count used by later premium-resource gates. B7 connects that analysis to Step-4 First Main for Draw: the next die is valued by expectation and best legal expected placement without consuming mechanical RNG or pre-committing the eventual post-roll row. B8 reuses the shared placement analysis after the roll: the actual value, fresh Battle context, and current legal rows determine placement, with exact tactical ties delegated to strategy RNG and rule-mandated placement kept non-strategic. B9 provides normalized own and public-opponent Support capacity, including caller-supplied relevant Live-Threat capacity and hidden-Wisp boundaries. B10 adds cumulative ordinary Support reachability from legal Bees and expected keep-better Butterfly improvement, plus a fresh continuation assessment that combines the meaningful-Strike-VP path with the later direct-Support usefulness result. Plant/Round intrinsic scoring remains intact; target-dependent Plant/Effect tactical projection is intentionally deferred to the later action/target-alignment checkpoint. Battle is **not yet certified**.

Battle currently exposes three strategy hooks:

1. `chooseFirstMainAction`
2. `chooseTurnAction`
3. `chooseDiePlacement`

The engine already separates decisions made before a random result from placement/result choices made after that result becomes known.

## 1. Human Baseline boundary

Battle is intentionally tactical but not deeply strategic.

Human Baseline asks:

> Given the board exactly as it is now, what immediate action most improves my Battle position?

It may recognize that acting later is useful and may spend worthwhile Support before committing Final Main. It does **not** simulate opponent-response trees, bluff, plan several actions ahead, forecast Doom, deliberately take a Wound to obtain a later Refresh, or conserve strong Battle resources for hypothetical future Battle rounds.

Before random information exists, use expectation. After the actual result exists and the rules present another choice, use the actual result and a fresh `DecisionContext`. Hypothetical strategy analysis never consumes mechanical RNG.

## 2. Row facts: Strike reality versus future response

Battle assessment must distinguish two opponent concepts.

**Score Benchmark** answers:

> What opposing score must I actually overcome?

It uses all participating opponents, including opponents who are Done.

**Live Threat** answers:

> Who can still change this row after I act?

It uses participating opponents who have not completed Final Main.

A Done player still participates in Strike resolution, still has dice/Critters that count, and remains a legal target when an effect allows it. Done is not withdrawal.

### Multiplayer winner semantics

Use the actual Strike rules.

For participating players:

```text
high = highest total
highPlayers = everybody at high
```

If every participating player is tied at high, nobody wins. Otherwise every high player wins.

Thus:

```text
10 / 10 / 7  -> the two 10s are winners
10 / 10 / 10 -> nobody wins
```

Never derive `currentlyWinning` merely from `scoreMargin > 0`; a shared winner may have margin 0.

### Planned row facts

A future shared row assessment should expose facts conceptually equivalent to:

```text
row / availability
own total
participating players
actual high/winner state

score benchmark player IDs
score benchmark total
score margin

live threat player IDs
live threat total
live-threat margin

wound state
secured-for-now
potentially hopeless
```

These are observations, not `PriorityScore`s.

## 3. Done state

Done must become authoritative Battle-round engine state rather than strategy memory or coordinator-local knowledge.

A player becomes Done only after Final Main has completely resolved, including any resulting target, branch, or die-placement decisions.

Done is player-level state, persists through the current Battle, and resets with the next Battle. Human Baseline reads it through immutable decision context.

## 4. Secured and potentially hopeless are temporary assessments

Current policy defaults:

```text
secured live-threat lead = 10
potentially hopeless score-benchmark deficit = 10
```

A row is secured-for-now when the actor is currently winning and either no opponent can still act or the actor leads the strongest Live Threat by at least 10.

A non-winning row is potentially hopeless when the Score Benchmark exceeds the actor by at least 10.

Neither state is memory. Recalculate after every actual action. A powerful current effect may make a previously hopeless row immediately relevant.

Do not store `securedRows` or `abandonedRows`.

## 5. Battle Swing

Human Baseline values immediate row changes using five named positive transitions.

With the default transition scale of 100:

| Transition | Tier | Base |
|---|---:|---:|
| `WIN_FLIPPED` | 5 | 500 |
| `TIE_ACHIEVED` | 4 | 400 |
| `WOUND_PREVENTED` | 3 | 300 |
| `WOUND_CREATED` | 2 | 200 |
| `SECURED_CREATED` | 1 | 100 |
| `NONE` | 0 | 0 |

These are tactical Human Baseline values, not VP.

### Transition definitions

`WIN_FLIPPED`: actor was not an actual Strike winner and becomes an actual Strike winner.

`TIE_ACHIEVED`: actor was non-winning with negative Score-Benchmark margin and reaches margin 0 while still not winning under the multiplayer rule.

`WOUND_PREVENTED`: actor would receive a Wound before the change and would not afterward, provided no stronger transition applies.

`WOUND_CREATED`: actor was winning but not leading the closest participating opponent by the Wound threshold and afterward does.

`SECURED_CREATED`: `securedForNow` changes from false to true and no stronger transition applies.

### One transition base per row

Never stack transition bases within one row.

Example:

```text
-6 -> +2
```

crosses several conceptual boundaries but receives only:

```text
WIN_FLIPPED base 500 + raw swing 8 = 508
```

### Raw swing

For ordinary Strike-result transitions:

```text
rawSwing = afterScoreMargin - beforeScoreMargin
```

For `SECURED_CREATED`, use the Live-Threat margin change when that is what created secured status.

If no named transition is crossed, ordinary positive or negative margin movement remains a low-level raw value.

### Negative/collateral movement

A harmful change is the negative of what reversing the same movement would score.

Example:

```text
+2 -> -2
```

is the negative of `-2 -> +2`, so it contributes `-504`.

This allows swaps and multi-row effects to account for collateral damage without a separate adverse-transition hierarchy.

### Multi-row effects

Calculate one contribution per affected row and sum them. Within a row use only the strongest transition; across different rows all contributions count. There is no default multi-row cap.

Use `Double` internally so expected values such as D8 = 4.5 remain precise until the final `PriorityScore` boundary.

## 6. First Main

First Main is immediate tactical play. It does not use Support-count tempo or acting-last reasoning.

Every legal Main candidate participates:

```text
Draw
Activate Plant
Round Effect 1
Round Effect 2
```

Conceptually:

```text
intrinsic action/card value
+
best immediate expected Battle Swing
```

Existing card/round-effect scoring remains useful for non-grid benefits such as VP, resources, or refresh.

Human Baseline normally plays a strong visible Plant now rather than saving it for Final Main. It does not create multi-action plans such as “play this Plant now, later Worm it, then replay it.”

### Plant/Effect targeting

For a target-dependent action, evaluate currently legal targets/branches one step deep. The downstream Effect decision must use compatible Battle analysis so the target that justified choosing the action is normally the target selected.

If genuinely new information becomes known before the legal downstream decision, reevaluate from that new state.

## 7. Random-information contract

Respect game timing.

If a target is legally chosen before randomness, choose it using expected value and remain committed.

If randomness is revealed before a later placement/target choice, use the actual value in that later choice.

### Draw

Before Draw:

1. determine the next die size under normal Supply/Discard rules;
2. use its expected roll;
3. try that expected value in all currently legal rows;
4. use the best expected placement to value Draw.

This does not pre-commit the eventual row and consumes no mechanical RNG.

Current engine order already provides the desired actual-resolution seam:

```text
draw
roll
resolve Roll Reward
build fresh DecisionContext
choose actual placement
```

Pre-draw Human Baseline does not probability-weight possible Critter/Wisp Roll Rewards. If one occurs, it becomes part of the fresh post-roll state naturally.

### Actual die placement

Use the actual rolled value and fresh context. Try every currently legal Strike Row, respect closed/withdrawn state and the 3-die Strike-Square limit, and choose the highest Battle Swing. Exact tactical ties use `StrategyRandomizer`.

If rules force a particular row, do not invent a placement choice.

## 8. Support / Final Main continuation

Step 5 repeatedly asks the active player to use one Support or take Final Main and become Done.

The core Human Baseline question is:

> Is there still a Battle worth fighting, and do I have at least one Support worth spending before I commit Final Main?

Final Main is an ending commitment, not an action that receives a generic positive bonus for ending participation.

Human Baseline normally takes Final Main when any of these is true:

- all relevant rows are already secured;
- all non-secured rows are effectively hopeless with current visible resources;
- no Support moves remain;
- Supports remain but none are worth spending;
- no row has a worthwhile immediate or cumulative improvement path.

At every pass, recompute the best Final Main from the current board. Do not remember an earlier planned Final Main.

## 9. Cumulative Support reachability

Human Baseline may make one limited aggregate judgment:

> If I commit the ordinary Support I visibly have, can I improve this row enough to matter?

This is not a search tree and does not model opponent responses or Support ordering.

Primary cumulative resources:

- Bees: multiple Bees may be added using current Bee value;
- face-up Butterflies: may contribute expected keep-better reroll improvement.

Do not generically add Worm, Water, Mulch, or Wisp into this pool; those use dedicated rules.

`BattleSupportReachability` implements this deliberately limited estimate. It
projects every legal Bee at the actor's current Bee value and allocates legal
Butterfly moves by expected keep-better gain without rolling. Repeated Butterfly
potential is capped by visible die headroom, and secured rows are omitted from
further cumulative commitment analysis.

A separate Strike-VP impact measure is used as a spending/reachability gate.

Current default:

```text
minimum meaningful Strike-VP gain = 2
```

An individually worthwhile Support can still justify continuing even when cumulative +2 VP is not reachable.

`BattleContinuationAssessor` combines these facts on a fresh Step-5 snapshot. It
reports Final Main when there are no relevant rows, every relevant row is secured,
no Support moves remain, or neither an individually worthwhile Support nor a
meaningful cumulative path exists. It reports continuation for either worthwhile
case. Direct/enabling Support analysis supplies the individual-usefulness input in
B11/B12; selection between the surviving Support and Final Main remains B14.

## 10. Support capacity and tempo

Count Support **moves/resources**, not generated target choices.

One Water is one move even if it has several legal reroll/refresh targets. One Worm is one move even if it has many placement/Flip targets.

Opponent capacity uses only public information. Hidden Wisp identities are not inferred; each opponent Wisp can count as one potential Support move.

For generic tempo comparison, compare our Support capacity with the largest relevant Live-Threat opponent capacity rather than summing opponents into a coalition.

`BattleSupportCapacityAssessor` implements this normalized counting layer. It consumes the actor's current legal Step-5 choices for exact own capacity, derives opponent capacity from public resources and Battle state, returns zero for Done opponents, and accepts the relevant Live-Threat IDs from the later tempo/continuation layer. It does not itself decide which rows are close or which opponents are relevant.

Acting-last pressure may be represented categorically as `NONE`, `WEAK`, `MODERATE`, or `STRONG`, but it is a modifier rather than the primary continuation gate. Tempo can make a useful soft Support more attractive; it must not turn a useless/harmful action into a good one.

## 11. Resource-specific Support rules

### Critters

Pre-Battle protected Critter reserves are not Battle reserves. Human Baseline is willing to spend Critters to win the current Battle.

Critter placement uses the current effective Critter value, including round modifications.

Bee is the principal cumulative row-power resource.

Worm is primarily preserved for Plant refresh. Direct Worm +1 placement is normally used only when that single Worm produces at least the minimum meaningful Strike-VP gain (default 2).

### Butterfly

Before reroll, use expected keep-better value and Battle Swing. The actual original-vs-rerolled choice remains the separate Butterfly Result decision area.

### Water reroll

Water is valuable. Its target is chosen before the reroll, so use expected value. Baseline normally requires at least one positive named Battle transition before spending Water on a reroll; Water is not spent merely to stall.

### Mulch

Mulch is deliberately conservative. Use expected stored-die roll and expected best placement. Normally use Mulch only if that expectation produces `WIN_FLIPPED`. After the actual roll, choose actual placement from fresh state.

### Worm Flip

Treat Worm Flip as enabling Support. Hypothetically refresh a face-down Plant and evaluate the best immediate Final Main it would newly enable. Value the improvement over the best Main already available, not generic Plant preservation.

Do not intentionally flip a useful face-up Plant down merely for tempo.

### Water Refresh

Current default:

```text
minimum Battle improvement steps = 2
```

For this premium-resource gate only, count positive named boundaries crossed by the best newly enabled Main action. Multiple crossed boundaries in one row may count as several **steps**, even though Battle Swing itself still uses only one strongest transition base per row.

Prefer Worm over Water for an equivalent single-Plant recovery when appropriate.

## 12. Special Wisp/effect policies

Most Wisps use ordinary card-specific intrinsic scoring plus shared Battle analysis.

Three cases require dedicated target/policy helpers.

### Cross-player same-size die swap Wisp

Evaluate every legal swap using full affected-row Battle analysis and collateral damage. Normally spend it only when projected Strike-VP improvement is at least 2.

### Immediate Strike-resolution Wisp

This Wisp locks in a favorable row before live opponents can spend remaining Support.

Current defaults:

```text
minimum lead over every participating opponent = 5
minimum Support moves on each relevant live contender = 3
```

Normally use it when both are true. Done opponents do not count toward the remaining-Support gate.

### Two-step die upgrade

Choose the legal current die that produces the highest legal resulting die size after the effect's two available upgrade steps. If several targets produce the same resulting die size, use expected Battle context to break the tie.

Target selection occurs before the replacement die roll, so use expectation. Later legal choices use the actual result.

## 13. Step-5 loop safety

Battle now has caller defense analogous to Cultivation:

- repeated per-player Step-5 decision-state detection;
- a generous hard ceiling of 100 Step-5 decisions per player.

The repeated-state signature contains the actor's legal choices plus a `DecisionContext` observation for every Battle player. That recognizes legitimate progress in active/Done state, grid state, withdrawal/closure, resources, Plant/Butterfly facing, Wisp state, and other information represented by decision context. Using all players' observations also prevents another player's private Wisp change from being mistaken for no progress, while the acting strategy still sees only its own legal `DecisionContext`.

Pass number is intentionally excluded from the signature because merely advancing a pass is not progress. The hard ceiling remains a last-resort circuit breaker for a malformed loop that changes observable state forever. These guards protect against broken strategy/effect loops without changing legitimate Support-heavy Battle behavior.

## 14. Code-shape preference

For focused single-purpose Human Baseline helper classes, prefer:

```kotlin
operator fun invoke(...)
```

when the class fundamentally performs one operation.

Examples of likely focused helpers include:

```text
BattleRowAssessor
BattleSwingEvaluator
BattleVpImpact
BattleActionAnalyzer
BattleSupportCapacity
BattleSupportReachability
BattleContinuationAssessment
BattleTempoAssessment
```

Dedicated special-effect evaluators should likewise remain small and composable.

Do not force `invoke()` onto established multi-method interfaces or APIs where a descriptive method is materially clearer.

## 15. Explicit non-goals for Human Baseline Battle

Human Baseline does not:

- forecast Doom;
- deliberately take Wounds to obtain Cleanup Refresh;
- preserve Battle resources for future Battle rounds through long-horizon optimization;
- model opponent-response trees;
- bluff;
- reason through intermediate standings when Score Benchmark/Live Threat is sufficient;
- peek at future RNG;
- calculate exact cyclic pass-order tactics.

Those belong to later Tactical/Strategic strategy levels.
