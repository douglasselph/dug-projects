# Battle pacing A/B — policy parity and research roadmap

**Baseline:** v37.03. **Experimental ruleset:** three sequential Strike Engagements.
**Source reviewed:** `leaf(20261030-211834).tgz`, and 10-sample final-Battle
human-guided output from 2026-10-30. **Status:** research only; not adopted rules.

## User-directed behavior contract

- Three Strike Rows remain. At each Engagement, determine the starting actor by
  the current row's highest showing die, then act clockwise. Freeze the order.
- Each player has two **normal** Main tokens across the Battle, at most
  **one normal Main per Engagement**. A Main may affect any *unresolved* row
  when allowed; a Main used during High need not target High.
- If two normal tokens remain for Middle/Low, use one in Middle and one in Low
  **when a legal Main exists**, even when the current action preview is weak.
- Candidate B plans against the *entire unresolved grid* at the start of each
  player's Engagement. Reassess after High resolves, then after Middle resolves.
  Never reconsider or modify a locked row.
- Up to two Support Actions *per Engagement*, subject to legality and resources.
  Sunlight is a Support granting a Main without using a normal token.
- Preserve v37 Main action/target decisions where legal, and v37 Wisp hold
  policy and Support continuation reasoning. Never consume mechanical RNG in
  hypothetical planning.
- No change to canonical v37 or detailed C04 calibration; respect A03.
- Exclude early-Resolve Wisp from both research decks. No effect, including
  Wispquake, may change a resolved row.

## Inspection results

The 10-pair, four-player final-Battle Human-guided pilot recorded 80 normal
Main Actions in each arm, 199 baseline Supports and 190 Candidate B Supports.
Candidate B allocated normal Mains High=32, Middle=30, Low=18 (40 players).
This is no longer mechanically fixed to High/Middle. The present timing layer
nevertheless forecasts *only expected Draw placement*. That is insufficient
for v37-equivalent Main timing when Plant and Round effects compete. Candidate
B also does not yet reproduce the full B10–B14 Support continuation gate.
The pilot's B-A Battle VP/player = +0.275 (n=10) is exploratory, not evidence
of balance or human duration.

## Sequence of tasks and acceptance gates

### Task 1 — Make Main allocation contract observable (IN THIS PATCH)

- Record a frozen decision per actor/Engagement: active rows, Main tokens left,
  expected-Draw row scores, planned rows, spend/defer, and forced reason.
- Retain canonical v37 First Main and Final Main action selectors.
- No fictitious Main plan when both normal tokens are spent.
- Pure constraints/recomputation tests and a real seeded Human-guided trace
  regression. Export additive `main_plans.csv`, retaining old CSV layouts.
- Clearly label the Draw-only timing proxy as provisional.

**Gate:** full Kotlin compilation and tests; a one-pair 4-player final Battle
produces 12 plan observations, executes eight normal Main actions, and
recomputes the available rows after each lock.

### Task 2 — v37 tactical Main valuation with action-specific previews (IMPLEMENTED AS RESEARCH HEURISTIC; full parity NOT YET VERIFIED)

- Reuse `HumanBaselineBattleStrategy`, `BattleMainPriority` and actual v37
  target/action analyzers to rank legal Draw, Plant and Round-effect actions
  *across all unresolved rows* before committing an Engagement token.
- Distinguish expected pre-roll projections from post-roll decisions; do not
  consume game RNG during projections.
- Model multi-row and resource/non-row actions explicitly. Identify unmodeled
  card effects instead of assigning arbitrary VP. Preserve genuine card value,
  not only expected Draw impact.
- Keep the plan fixed through the actor's Engagement; execute the v37 Main
  action/targets only when legal, then recompute at next row's Engagement.
- Regression scenarios: Plant vs Draw, Round vs Draw, prospective future row,
  forced Middle+Low, immediate Wound altering the next plan, and row locks.

### Task 3 — v37 Support continuation under the two-Support window

- Adapt B10–B12/B14 capacity, reachability, and enabling-Support analysis to
  two Supports *per Engagement*, Sunlight-funded Main, and interleaving.
- Avoid relying only on individually positive Support scores. Test Wisp
  hold/play, Refresh, Mulch, Critter chains and support-to-Main combinations.
- Compare canonical v37 Support decisions with Candidate B in otherwise
  identical open-row states; explain every difference from timing constraints.

### Task 4 — Fidelity and failure-path acceptance

- Test cross-row Plant effects, swaps, movement, Wispquake, Critter boosts,
  immediate Wounds, row locks, Doom. No illegal locked-row option offered.
- A03 checks, full focused Kotlin regressions and seeded end-to-end cases.

### Task 5 — Credible tactical-policy paired trials

- Full Human Baseline for the identical legal upstream history; 2/3/4 players,
  Battles 1/2/3, abundant and asymmetric Support inventories, strong Plants.
- Mechanical Control is only a correctness control, not pacing intelligence.
- Record policy parity, action choice, Main commitment and Support utilization.

### Task 6 — Battle decision-cost / passive-downtime measurement

- Instrument A and B identically: handoffs, physical operations, decision
  windows, reassessments, branching, passes, and remaining meaningful options.
- Model post-final-action waiting and long inactive stretches under fast/base/
  slow human-time assumptions; report percentiles and reversal conditions.
- JVM CPU time is **not** a human Battle duration estimate.

### Task 7 — Turn-order fairness and design risk

- Compare equivalent *last meaningful actor* wins in both A and B; adjust for
  initial row power and seating. Add controlled order counterfactuals.
- Test early-Wound cascades, stranded Supports, VP and Plant-engine effects.
- Report uncertainty rather than a categorical design conclusion.

## Launcher and result-handling rules

`doc/battle/run_battle_pacing_ab.sh` currently calls `writeSimulationRuntimeClasspath`
once, then one direct-Java JVM for the entire requested batch. Preserve its
seeds/process scope and publication of both successful/failed `.tgz` result
archives to `~/Downloads`. Never revert to Gradle-per-game.

To verify this task after installing the changes-only ZIP:

```bash
python3 bin/a03_verify_boundaries.py
./gradlew compileSimulationKotlin
./gradlew simulationTest --tests '*EngagementMainTimingPlannerTest' --tests '*BattlePacingABBatchTest'
bash doc/battle/run_battle_pacing_ab.sh --samples 1 --players 4 --battles 3 \
  --policy human-guided \
  --out output/experiments/battle-pacing/policy-task1-human-final-1
```

Inspect `main_plans.csv`, `opportunities.csv` and `summary.csv`. Do not merge
these traces with the previous policy-version results.

## Task 2 implementation note (October 31, 2026)

Task 2 replaces the pilot's expected-Draw-only *timing* input in the
`human-guided` Candidate B policy with `BaselineMainActionForecaster`.
For each legal Draw / Plant / Round-effect Main, it evaluates the existing v37
First/Final Main score, including owned-card influences, on the actual immutable
context; it also calculates per-row sensitivity using an immutable
counterfactual in which just that row is open. Unmodeled/non-row contributions
remain explicitly separate (`noOpenRowScore`) and are not treated as Strike VP.
The per-row max across legal actions allocates the two normal Main opportunities.

**What this does NOT claim:** hypothetical rows are not full future game states;
projecting already-legal actions against hypothetically locked rows is a timing
approximation, not a legality oracle or a perfect multi-effect lookahead.
If the canonical scorer treats an effect only through an intrinsic/fallback
priority, its per-row sensitivity will be zero and this is visible in
`main_plans.csv`. Actual action execution/targeting still use the canonical v37
policy on the actual *current* open-row context. Existing First-vs-Final Main
ordering remains in place. The eventual parity acceptance requires seeded
traces of Plant/Round choices and multi-row interactions against v37. The
separate B10–B14 Support parity analysis is Task 3, not addressed here.

A/B statistics generated before this Task 2 patch belong to a different B
policy version and must not be aggregated with the new policy results.

Task 3 follows immediately: v37 Support continuation under up to two Supports
per Engagement, including enabling chains and Sunlight.
