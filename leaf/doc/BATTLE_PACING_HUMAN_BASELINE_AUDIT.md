# Battle Pacing — Human Baseline decision-path audit

Source: `leaf(20261030-205153).tgz`, and supplied `human-guided-final-10` output.

## Findings

1. `--policy mechanical` explicitly creates `PlayerDecisionFactory.mechanicalControl()` on every seat and selects `MechanicalEngagementPolicy` for Candidate B. These are complete legal games but **not** Human Baseline games. Mechanical Battle chooses Draw where legal, then Final Main; it deliberately doesn't spend Supports. The experimental Mechanical policy chooses one Main then Pass.
2. `--policy human-guided` creates `PlayerDecisionFactory.humanBaseline()` on every seat **for both paired prefixes**, then uses production `battle` policy for A and `HumanGuidedEngagementPolicy` for B. Both arms are legal real-game continuations from matching canonical openings.
3. The Human factory supplies `HumanBaselineBuyStrategy`, `HumanBaselineBattleStrategy`, and the Human Battle Main/Support seam policies. The Buy strategy delegates to `HumanBaselineBuyPlanner`: short-horizon (up to two purchases) partitioning of current Hand dice, projected Grove resources/graft legality, payment evaluation and critter budgeting. The Battle strategy uses scored First Main, Step-5 continuation, B7/B8 placement, B9-B12 Support analysis, and plant/effect targeting. This is substantive decision logic, not a stub.
4. A source scan of the Human Baseline `buy/` and `battle/` packages found no `TODO`, `FIXME`, `NotImplementedError`, or intentional stub implementation. Both Human strategies delegate to Mechanical only when `DecisionContext.EMPTY`. The normal `BattleActionCoordinator` constructs `DecisionContextFactory.create` for every production Battle decision. This is a source audit, not proof that every card/effect has ideal strategic calibration.
5. The v37.03 tactical score is NOT literal maximum final VP. It uses `BattleSwingEvaluator` transitions (500 win flipped, 400 tie achieved, 300 Wound prevented, 200 Wound created, 100 secured), raw margin shifts and intrinsic Card/Round scores, expected Draw effects, and downstream targeting. This was intentional and is described in `doc/HUMAN_BASELINE_BATTLE.md`.

## 10 paired 4-player final Battles — observations

* A: 80 normal Main, 199 Support actions.
* B: 80 normal Main, 186 Support actions.
* B row Main distribution: High 40, Middle 40, Low 0. This is NOT evidence of successful row-wise Main allocation; the original B adapter forced First Main at first opportunity in High, then unconditionally chose the second Main in Middle.
* B row Supports: High 78, Middle 68, Low 40. Real Support policy is being called. However, it is **not equivalent** to the complete v37 Step-5 continuation contract: B used `BattleSupportPriority.score>0` filtering, plus the wisp hold/play gate, and only calls `chooseTurnAction` when legal Mains remain. B could select Supports after consuming its second normal Main; v37's Final Main terminates Support turns. Different turn structures are expected; identical strategy quality is not established.

## Research-only timing correction, limited scope

The included experimental `EngagementMainTimingPlanner` makes the **decision WHEN** to spend a normal Main per row. It recomputes after every resolved Strike, and never commits more than one normal Main per Engagement or more than two in total. It uses v37 B7's expected Draw placement projection to compare unresolved row opportunities. The existing Human `chooseFirstMainAction` and `chooseTurnAction` still choose WHAT action; actual effect/placement targeting still uses production rules.

**Critical limitation:** The scheduling proxy is Draw-based, not the complete projected effect of every Main Action. It is a pilot correction to the plainly broken always-High+Middle schedule, **not** a fully equivalent v37 Main planner, and must NOT be treated as a validated strategic/balance policy. In particular, multirow Plant effects, Round effects, and Sunlight-funded Mains need action-specific timing projection and controlled tests. The original A/B outputs are historical and must NOT be silently compared with results from this changed B policy.

## Next acceptance gate

1. Run `BattlePacingHumanWiringTest` and `EngagementMainTimingPlannerTest` plus the existing Battle pacing tests.
2. Pilot `human-guided` again and inspect `opportunities.csv`: at least one initial state should defer High or Middle to Low, and no player may use more than one normal Main per Engagement or more than two in total.
3. Add explicit per-action projected Main scores and intended Strike-row impact BEFORE claiming v37-quality tactical equivalence. Integrate v37 Support continuation/reachability under Candidate B's two-allowance rule and test multi-step Support chains.
4. Only then measure tabletop decision windows, handoffs, downtime and last-actor fairness symmetrically for A/B.

Do not restart C04 general calibration; this is a focused experiment-policy timing audit.
