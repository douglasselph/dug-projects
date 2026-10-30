# Task 5B.1 — Human Baseline Scoring Contract

Status: **design specification; no implementation changes**  
Source reviewed: `leaf(20261022-203432).tgz` (October 22, 2026).  
Scope: Human Baseline candidate evaluation, including Plant effects, targets, Buy, Cultivation Main, Battle Main, and bounded nested effects. This document is the proposed contract for Tasks 5B.2–5B.12, **not a claim that the current code already implements it**.

## 1. Governing principle

Score **a complete legal decision in the current observable state**, based on the **incremental expected consequences of that decision**, compared against the same starting state. Rank alternative decisions on a **common scale within the same decision point**. An effect label is not itself a benefit; concrete consequences are. Avoid adding fixed effect-identity bonuses on top of benefits that have already been projected.

A legal action may be good in one state and poor in another. Human Baseline remains a deterministic, explainable heuristic and is not assumed to make optimal choices; its representation must nevertheless consider each meaningful legal option and its material consequences.

## 2. Public information and uncertainty

1. Use only state visible to the acting player at the decision time: own and exposed dice, locations, Plant facing and identities, supplies, opponents' public positions, legal actions, Round effects, current VP, and known future schedule as permitted by the existing decision context. Never peek at future RNG, hidden deck order, or another player's private information.
2. **User-specified arithmetic-mean rule:** A hypothetical unknown roll of an N-sided die is represented by the real number `(N + 1) / 2`. Thus D4=2.5, D6=3.5, D8=4.5, D10=5.5, D12=6.5, and D20=10.5. This applies consistently to hypothetical Draws and rerolls. If an outcome is already revealed, use its actual face value instead.
3. Fractional projected values are **evaluation-only**. They must never create or mutate an actual `Die`, be written into `Dice Hand`, be converted to an integer die face as part of the hypothetical engine, or cause the runtime game to roll differently. The real executor still rolls normally.
4. A hypothetical mean value does **not** imply that an exact face (1, 2, 3, or 4) was rolled. Do not trigger a Roll Reward, exact-face conditional, equality check, or die-face manipulation as if 3.5 were a legal face. A separate expected-roll-reward model may be added later *if explicitly specified and attributed to the same probabilistic outcome*, but it must not be accidentally inferred from the mean; until then mark rewards/face-conditioned follow-ons as **not projected** and disclose the limitation. Do not silently award zero expected rewards and claim a complete projection.
5. For arithmetic continuous quantities, keep `Double` through scoring. For nonlinear/discrete rules (purchase thresholds, winner/tie/wound thresholds, caps, legal conditions, hand limits, scoring-per-die-showing-1, and so on), evaluating the mean **does not in general give the expected outcome**. Following the user's rule, the reference model evaluates the mean as a representative estimate, but must label discrete conclusions as *mean-state proxy* rather than an exact statistical expectation. Do not silently floor/ceil 3.5 into 3 or 4 or claim a guaranteed purchase/win.
6. Known effects remain deterministic: for example, `Raise +2` on a face 3 die means face 5 (capped by die sides); when the underlying die is only hypothetical, cap `min(sides, mean + 2)` for evaluation without creating a physical die. Rerolling a revealed die replaces its observed face with the die's arithmetic mean in a hypothesis.
7. Do not consume or reset any game RNG when evaluating choices. Repeated evaluation of the same candidate in the same state must yield the same result.

### Representative die reference

| Die | Evaluation mean |
|---|---:|
| D4 | 2.5 |
| D6 | 3.5 |
| D8 | 4.5 |
| D10 | 5.5 |
| D12 | 6.5 |
| D20 | 10.5 |

## 3. Candidate generation and legality

- **First enumerate legal actions** using the actual engine's eligibility and capacity rules; don't score illegal options or fix an illegal option by silently changing its effect. Separate 'effect activation legally available' from 'particular target or sequence legally available'.
- Candidate includes action identity, effect code, source card, affected players/zones/dice, placement, choice sequence, Main/Support timing, and the resources it consumes. Stable candidate IDs are needed for tests and explanations.
- If an effect presents choices, evaluate each legal branch. The scorer must not invent variants or select an unavailable target. A `Done`/pass option must be represented wherever permitted, including early completion of multi-choice effects.
- The executor remains authoritative. Projection must mirror its legality; mismatch between projected and executable choices is a test failure, not an occasion to force execution.
- The next choice of a sequential effect is generated against the **projected state after the earlier choice**. For stochastic consequences, represent the unknown outcome using Section 2 rather than simulating a lucky roll.

## 4. What a hypothesis contains

The preferred target model for later implementation is an **immutable, side-effect-free projected state** (or a strictly scoped copy of the public decision view) with explicitly typed changes to:

- dice: ownership, zone, placement, rolled/expected face, discard/draw/trash, changed value;
- resources: Bees/Worms, Water, Mulch, Wisps, Butterflies, VP, available supplies;
- creature: grafts, Plant facing, active effect stack, Refresh eligibility;
- Battle: per-player Strike Row totals, occupied squares, legal future placements, provisional winner/wound/VP assessments;
- Cultivation: current purchasing resources, individual purchase affordability, remaining action choices and future dice cycling;
- game progression: current phase, actions remaining, known rounds remaining, retained resources.

The hypothesis must never mutate the live game state or advance action execution. Derived score terms carry a provenance key identifying the *underlying consequence* so that different evaluators cannot claim it twice.

## 5. Relative benefit and common score units

For candidate A in observable state S:

`DeltaValue(A,S) = EvaluatedValue(Project(S,A)) - EvaluatedValue(S)`

This is the **incremental consequence**, not total board strength after A. When comparing candidates from the same decision point, compare their `DeltaValue` and opportunity implications using the same phase-sensitive valuation function. If absolute `PriorityScore` baselines are retained for compatibility, they must be calibrated to the same reference for all candidates of that *decision family*; no card-specific base may double-count projected value.

Recommended **conceptual** decomposition (not fixed coefficients yet):

`candidate score = immediate incremental value + retained/future incremental value + opponent-impact value - incremental resource expenditure - incremental opportunity loss`

- **Immediate:** expected Buy affordability and spendability *this round*, current Battle row swing/VP/wound risk, current VP gained, Draw resources now available.
- **Future:** dice newly owned or deferred to Discard/Supply, Plant availability/Refresh implications, retained Helpers, end-game scoring deltas, future actions enabled. Discount or cap future benefits consistently; do not invent arbitrary multipliers before a calibration task.
- **Opponent impact:** denial of opponent's active die in Battle/Hand or long-term die ownership, scored from the opponent's observable before/after state. Do not assume a Discard die has the same immediate threat as an active Battle die.
- **Expenditures:** dice consumed, Critters spent, facing changes, lost resources, loss of an actionable die, and explicit action-resource payments.
- **Opportunity:** evaluate the competing action at the same decision point (Draw, Plant, Round effect, or Support/finish). Do not blindly subtract 'one Main Action' from every Main candidate if the comparator already prices one Main Action for each; this would count action cost twice.

The same evaluator should rank both **whether to activate an effect** and **which legal targets it should use**; the top-level activation value is the best legal full sequence, not a generic bonus plus the selected nested effect's full score.

## 6. Avoid double-counting: source-of-value ledger

Each projected outcome contributes its utility **once**. A recommended implementation carries a consequence ledger, keyed by observable state change and beneficiary. Before adding a term, ask whether it is a separate benefit or a restatement of an already-accounted consequence.

| Potential overlap | Correct treatment |
|---|---|
| New D4 owned + D4's printed end-game VP | These may be distinct: ownership enables future rolls, and card's end-game scoring gains +1 if the D4 remains owned. Record distinct `futureDieUse` and `endGameVP` consequences, with no second generic `gainD4 +15` bonus. |
| D4 moved into Hand + Buy threshold crossed | Do not also apply a generic 'hand die' bonus that already measures the same purchasing gain; separate residual future ownership value if justified. |
| Battle Raise +2 + improved Strike/wound odds | Prefer one Battle position delta from **after** the Raise and all collateral changes; do not add a separate `+2` tactical bonus *and* full row swing for the same change. |
| Stolen opponent die + opponent's lost Strike total | Count opponent denial once in the Battle swing, not both as a steal premium and a separate negative-row evaluation. |
| O Edelweiss replays another Plant | Score the nested Plant's **incremental resulting state**; don't add its generic card score again on top of the result. |
| Two chained Plants touch the same die | Apply sequentially and cap at actual sides; don't sum independent uncapped increases. |
| Gaining future resource + later expenditure in a chain | Record both as successive transitions; count only what remains or was actually converted into another benefit at chain end. |
| Purchase affordability vs actual purchases | Crossing a cost tier is *option value* until Buy occurs. Don't count both the full VP of a hypothetically purchased card and a tier bonus representing that same purchase. |

For implementation, expose a score breakdown by **distinct consequence IDs** (`immediateVP`, `battleSwing:row`, `buyOpportunity:tier`, `futureDieUse:die`, `endGameVP:petalD4`, `resourceDelta:type`, `opponentSwing:player`) plus any explicitly labeled heuristic residuals. Equal or overlapping IDs should be merged, not blindly summed. These names are illustrative, not committed APIs.

## 7. Cultivation valuation

- Use visible die values and Critters to evaluate purchase capability. Follow Buy's real rules: **each die pays one purchase, overpay is allowed**, and purchases consume their payment dice; additive aggregate purchasing power is a diagnostic shortcut, not proof that a particular set of items can be bought.
- A known Draw's unknown value is represented by arithmetic mean; D6→3.5. Use fractional power in the *proxy*, but compare affordability carefully: `3.5 >= 4` is false and `3.5 >= 3` is true in the mean-state model; neither asserts the real roll will or won't afford an item with certainty. Preserve accompanying uncertainty annotation.
- Separate immediate Buy option improvement from retained value of a die that goes to Discard. A newly gained Hand D4 may improve Buy, may be cycled by Petal, and may no longer be retained in Hand after the chosen sequence. Evaluate its **final projected location**.
- The current `DrawPriority` uses a fixed base plus expected value of next lowest available die; its `DrawReferenceObservation` includes purchase tiers but they do not currently modify the score. 5B.3+ may use these observations in a shared projection, but 5B.1 does not assert the current Draw method already follows this contract.

## 8. Battle valuation

- Use a common before/after **whole affected grid** assessment. For all changes, include legal placement, own and opposing die values, Strike leads/ties, prospective win VP, Wound risk, row capacities, and effects on remaining legal actions. Don't price a Raise and the resulting Strike swing separately.
- Use opponent-facing information only. Project pending Battle resolution according to actual strike/wound rules. Identify the metric as **provisional** if future player actions could change it.
- If an effect affects two Strike Rows (Sapping alternate anchor), calculate the joint final board position, not the sum of two analyses that both reuse the same original state.
- A die added to Hand in Battle requires legal Strike placement. If none is legal, the alternative is illegal when the effect specification says so; don't silently reroute to Discard.
- Draw/roll use the arithmetic mean of unknown die faces. A mean is not a literal placed die value: the evaluation layer handles it without mutating a Battle square to a fractional physical die.

## 9. Sequential/nested effects (O Edelweiss)

The **intended rule newly confirmed by the designer** supersedes the previous two experimental eligibility choices; implementation is reserved for 5B.2:

> Twice, optionally choose: (1) Play **another face-up** Plant and then spend/flip it face down in the normal manner, or (2) Flip another Plant. Recompute legal choices after the first fully resolves; `Done` may stop early.

- Face-down Plants **cannot** be Played until first Flipped up. A face-up Plant Played through Edelweiss becomes face down; it cannot be Played twice without another flip. Edelweiss may not target itself or re-enter the active effect stack through recursive chains.
- Enumerate first legal choices; project each; then enumerate legal second choices **from that projected result**. Account for every nested card's real effect/targets and resource restrictions. Plan must honor two-choice budget, recursion protection, legal capacity, and early Done.
- Example: two face-up Queen's Blossoms → Play first (Draw 2), Play second (Draw 2), each becomes face down; unknown die results use their means. Two face-down Queens → Flip one up then Play it (Draw 2), ends face down. No double use of the same face-up card without spending an intervening choice on Flip.
- The top-level score is the best **whole two-choice projected delta**, not the sum of two isolated initial-state evaluations and not the sum of two nested absolute `PriorityScore`s. Keep score and chosen plan consistent; at actual execution recompute legality after each completed choice.
- A Flip may have deferred strategic value even if it does not immediately execute a Plant; evaluate the resulting facing/Refresh opportunity without treating future execution as guaranteed.

## 10. Petal variants

Across P1–P4, preserve the common D4-discard-and-redraw stage and 1 VP per D4 owned at end. Enumerate **gain from available Grove D4** and all permitted opponent sources (Hand only or Hand+Discard); evaluate each legal source, own destination (Hand/Discard), resulting opponent state, and best legal optional D4 cycling sequence. A new Hand D4 may be eligible to be cycled immediately if the rules permit; a Discard D4 is not in Hand and cannot be cycled directly. During Battle evaluate loss from opponent Strike square, placement of gained D4, room constraints, and subsequent draws using means. Record uncertainty and do not blindly add a +15 acquisition heuristic on top of projected effects. Zero available Grove dice and zero eligible steal targets are genuine legality conditions; no imaginary D4 should be granted.

## 11. Sapping variants

- **S1:** select die A, Raise A +2. In Battle reduce opposing dice in A's Strike Row (minimum 1); the total amount actually reduced Raises A, subject to A's cap.
- **S2:** select die A, Raise A +2; select **a distinct die B**; in Battle reduce opposing dice in B's Strike Row; the total amount actually reduced Raises B, subject to B's cap.
- Cultivation is only the initial +2 Raise; there is no Battle row to affect. Evaluate all legal choices, caps, empty opponent rows, and both rows in S2 together. Existing S1 collateral projection can be a building block, but must not be copied as an incorrect one-row approximation for S2.
- An opposing die showing 1 contributes zero actual reduction; a die showing 2 contributes one; one showing 5 contributes two. Do not credit nominal reductions that were blocked by the floor.

## 12. Comparing candidate scores, ties, and timing

- Produce the score **before execution** from pre-action visible state; do not observe the real future die roll during projection.
- Use common scoring units across candidates at the same decision point. Document baseline constants and coefficients once in a shared calibration plan. Where there is no reliable calibrated coefficient, report the unweighted consequence as a diagnostic rather than inventing precision.
- Make scoring deterministic. For tied scores, use a stable, documented tie-break (existing action order or stable IDs), not an unseeded random tie-break in the projection layer.
- Treat Main, Support, and Buy choices as separate decision families; don't compare raw uncalibrated scores across them. When the chosen effect triggers nested choices, re-evaluate after each actual effect completes.
- Distinguish observed current advantage from incremental action-created advantage, and disclose incomplete future prediction horizons.

## 13. Audit record / expected API shape (proposal, not code)

Each evaluated candidate should be explainable with:

```
CandidateEvaluation {
  candidateId, effectCode, actionFamily, phase,
  legal, illegalityReason?,
  baselineStateFingerprint, predictedStateDelta,
  components: [{consequenceId, beneficiary, category, delta, utility, certainty}],
  totalPriority, scoreScaleVersion,
  sequenceChoices?, predictionNotes
}
```

`certainty` distinguishes `observed`, `deterministic`, `mean_proxy`, and `heuristic`. Future implementation need not use this exact class shape but must retain these distinctions in reports/tests.

## 14. Acceptance tests for later tasks

1. **Unknown-roll consistency:** D6 Draw and D6 reroll are represented as `3.5` in evaluation, with no real die mutation, no RNG advance, and no Roll Reward triggered by the proxy value. D4=2.5, D20=10.5.
2. **Legality:** candidates with no available D4 or no eligible opponent steal targets cannot be scored as successful acquisitions; full Battle capacity correctly excludes Hand-destination acquisition where the effect requires it.
3. **State delta:** unchanged board elements produce zero incremental value; awarding 2 VP generates one 2-VP benefit, not both a card bonus and a VP benefit.
4. **Battle no double count:** one Raise causing one row swing is scored once, even if multiple analyzers can observe it.
5. **Sequential cap:** two Raises to a die near its maximum credit only the actual final improvement.
6. **Petal source/zone:** Hand and Discard destinations and stealing Hand versus Discard produce distinct projected effects whenever their actual consequences differ.
7. **Edelweiss:** face-down Play unavailable; Flip→Play legal; Play(face-up)→Play(same card) unavailable without Flip; two face-up Queens both executable and end face down; recursion forbidden; `Done` legal.
8. **Snapdragon:** S1 and S2 affect correct row(s) and credit only actual reductions, including floor at 1 and die caps, with empty rows and full squares tested.
9. **Cross-decision consistency:** top-level activation and target selection use compatible consequence measures; selecting an effect does not lead to a target selected by an unrelated flat bonus.
10. **Repeatability:** repeated evaluation in the same state is identical and does not modify the current game; trace explanations identify each counted consequence and whether it is a mean proxy.
11. **Nonlinear expectation labeling:** a D6 mean of 3.5 against purchase cost 4 yields no *mean-state threshold crossing*, but diagnostic output does not claim the actual probability of affording cost 4 is zero.

## 15. Out of scope / handoff

- **No changes** to enums, `OEdelweissEffect`, Petal or Sapping handlers, baseline weights/scorers, CSVs, or game rules in Task 5B.1.
- **5B.2** first corrects Edelweiss to the designer's face-up-only Play rule and removes/retires contradictory experimental rules carefully, with migration and tests.
- **5B.3** designs and implements shared projected-state and outcome-ledger primitives, with `Double` proxy die values isolated from runtime physical dice.
- **5B.4–5B.7** consume those primitives for Petal, Edelweiss, Snapdragon, and top-level/target consistency.
- **5B.8–5B.12** audit remaining effects, calibrate and validate the baseline, and freeze a reproducible scoring version.

### Existing source touchpoints (for reference)

- `src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/DrawPriority.kt` — mean next-die roll and diagnostic observations.
- `src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/common/PurchaseThresholdHeuristics.kt` — current purchasing-power/tier model; be careful with one-die-per-purchase semantics.
- `src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/scoring/PriorityScore.kt` — explainable integral priorities, not yet a unified state-delta value.
- `src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/battle/BattleOEdelweissAnalyzer.kt` — currently explicitly **one-step** Battle analysis and no complete two-choice lookahead.
- `src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/battle/BattleOwnDieCollateralAnalyzer.kt`, `BattleDiePlacementAnalyzer.kt`, `BattleRowAssessor.kt`, `BattleSwingEvaluator.kt` — Battle valuation building blocks.
- `src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/card/CardScoringHelpers.kt` — effect-specific score adjustments requiring an overlap audit.
