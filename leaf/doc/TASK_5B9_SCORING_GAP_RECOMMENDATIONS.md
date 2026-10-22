# Task 5B.9 — Human Baseline consequence-valuation gap review

Source: `leaf(20261022-215946).tgz`, inspected directly; code paths include `CardScoringHelpers.playScore`, scorer registry, previous 5B.8 matrix, and the already-added Petal, Snapdragon, and O Edelweiss projectors. **Audit only: no code changes.**

## Counts and interpretation

- 48 catalog entries: 36 Plants and 12 Wisp CSV entries (the four Pollinating colors are separate entries).
- Existing 5B.8 coverage labels: 26 heuristic/incomplete, 19 partial/state-informed, 2 specialized tactical projections, 1 unclassified. These labels are not a fix count.
- Recommended candidates: **17 high-priority + 14 medium-priority = 31 effect-entry reviews/improvements**; 15 retain/verify for now; 2 special/event cases. These are review targets, not 31 independent new algorithms; many can share projection infrastructure.
- O Edelweiss is a *remaining completeness* item following earlier 5B.5 work, not a claim that its current planner is absent. Petal and both Snapdragon variants already have specialist projectors; classify those as verification/remaining alignment instead of rebuild work.

## Evaluation rule

Project exact deterministic state changes. For unknown rolls, use arithmetic mean (D4 2.5, D6 3.5, etc.) without rolling or triggering Roll Rewards; do not turn fractional predictions into legal die faces. Compare incremental value against the same pre-action state, counting VP, purchases, resources, opponent losses, and Battle swing once. Evaluate all legal targets and branch conditions; score the chosen downstream target by the same projected criterion used at activation. Fixed values may remain for genuinely diffuse future option value, when no concrete present consequence can be forecast, with an explicit named term.

## Per-entry recommendation matrix

| ID | Effect/card | Existing coverage | Priority | Recommended investigation |
|---|---|---|---|---|
| Root_05_01 | Root Double Down | Heuristic / incomplete | Medium | Project exact doubled value into useful Purchase/Strike deltas, not just raw pip gain. |
| Root_05_02 | Root Four More | Heuristic / incomplete | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Root_05_03 | Root on a Roll | Heuristic / incomplete | Medium | Estimate repeated reroll-to-3+ under the user mean rule without sampling and assess next Buy/Strike outcome. |
| Root_05_04 | Root Well | Partial / state-informed | Medium | Value Water’s marginal refresh/reroll choices and existing Water supply. |
| Root_07_01 | Root & Scoot | Heuristic / incomplete | High | Project raise and Battle withdrawal, including wound prevention and forfeited Strike VP. |
| Root_07_02 | Root Appreciation | Partial / state-informed | Medium | Project newly gained Worm and actual stacked boost this round (Buy/Strike context). |
| Root_07_03 | Root Down Payment | Partial / state-informed | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Root_07_04 | Root Recall | Heuristic / incomplete | High | Project replacement draws using die means and next available supply, with purchasing/row effects. |
| Root_09_01 | Root Awakening | Heuristic / incomplete | High | Project upgrade size, present mean-roll gain, supply availability and persistent ownership. |
| Root_09_02 | Root Cause | Partial / state-informed | Medium | Project selected die flip and Battle row/purchase change, including negative flips. |
| Root_09_03 | Root Kindred | Partial / state-informed | Medium | Project copy-to-value choice into Buy/Strike outcome, preserving die cap. |
| Root_09_04 | Root Loot | Partial / state-informed | Medium | Project chosen discard die Mulch timing and actual future access. |
| Vine_07_01 | Berry Important / Berry Tasty | Partial / state-informed | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Vine_07_02 | Shift Happens | Heuristic / incomplete | High | Project flip of own/opponent Plant, including next activation/refresh and opponent denial. |
| Vine_07_03 | Vine and Again | Heuristic / incomplete | High | Evaluate actual reusable spent Root/Vine effect through the shared effect evaluator. |
| Vine_07_04 | Vine Yield | Partial / state-informed | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Vine_09_01 | Low & Behold | Partial / state-informed | Medium | Project setting selected lowest die to max against Buy/Strike goals. |
| Vine_09_02 | Parting Thorn | Heuristic / incomplete | High | Project the chosen opponent’s real available card/Wound consequences; compare phase branches. |
| Vine_09_03 | Rise and Vine | Partial / state-informed | Medium | Project +2 to all own dice respecting caps and Battle row margins. |
| Vine_09_04 | Vine and Dine | Heuristic / incomplete | Medium | Include the concrete critter sacrificed and complete +5 effect; avoid independent reward double count. |
| Vine_11_01 | Reap What You Roll | Heuristic / incomplete | High | Project net discard-and-two-draw gain, correct available dice and Battle square capacity. |
| Vine_11_02 | Saplink Trellis | Partial / state-informed | Medium | Allocate multiple +1 Raises across targets to maximize actual outcome. |
| Vine_11_03 | Vine and Punishment | Heuristic / incomplete | High | Project reroll-on-3 branch and row reductions (the actual active research effect). |
| Vine_11_04 | Vine's the Limit | Partial / state-informed | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Flower_11_01 | Alluring Nectar | Heuristic / incomplete | High | Evaluate each available Butterfly identity, steal vs gain, refresh and opponent denial. |
| Flower_11_02 | Bloom Backbone | Partial / state-informed | Medium | Allocate multiple +1 Raises across targets to maximize actual outcome. |
| Flower_11_03 | Sapping Snapdragon | Partial / state-informed | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Flower_11_04 | Transplant Tulip | Heuristic / incomplete | High | Project mean-value Draw, forced die swap, +2 Raise and both affected Battle rows together. |
| Flower_14_01 | Bee-loved Bloom | Heuristic / incomplete | High | Compute actual Bee gains/steals and the +2 stacking value this round, with opponent denial. |
| Flower_14_02 | Bloom Backflip | Heuristic / incomplete | High | Project resulting flip of each eligible opposing die plus own Raise, based on exact die faces. |
| Flower_14_03 | Gust of Petals | Heuristic / incomplete | High | Use arithmetic means for rerolls and project eligible opponent rerolls and Strike outcomes. |
| Flower_14_04 | Petal To Die 4 | Partial / state-informed | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Flower_17_01 | Bursting Blossom | Heuristic / incomplete | High | First project best capped +1 Raise, then exact max-face count and mean-valued resulting Draws. |
| Flower_17_02 | Forget-Me-Not | Partial / state-informed | Medium | Project selected discard die mean roll and Battle square destination. |
| Flower_17_03 | O Edelweiss | Heuristic / incomplete | High | Finish nested two-choice state projection: resource/die modifications and nested target choices, not only facing. |
| Flower_17_04 | Queen’s Blossom | Heuristic / incomplete | High | Use two sequential mean-value Draws and legal Battle placement/purchase tier differences. |
| Wisp_Award_VP | Wisp of Honor | Partial / state-informed | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Wisp_Award_VP2 | Berry Patient | Partial / state-informed | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Wisp_Gain_Critters | Whispering Wings | Heuristic / incomplete | High | Value two chosen Critters and their present Buy/Battle use, not just generic gain base. |
| Wisp_Gain_Green | Pollinating Wisp | Heuristic / incomplete | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Wisp_Gain_Purple | Pollinating Wisp (Purple) | Heuristic / incomplete | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Wisp_Gain_Red | Pollinating Wisp (Red) | Heuristic / incomplete | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Wisp_Gain_Yellow | Pollinating Wisp (Yellow) | Heuristic / incomplete | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Wisp_Mulch_Die | Pocketed Spark | Partial / state-informed | Medium | Project stored discard die and expected timing rather than generic reserve score. |
| Wisp_Quake | Wispquake | Heuristic / incomplete | Special | Not a normal voluntary effect choice; verify event semantics and event-value handling separately. |
| Wisp_Reckoning | Wisp_Reckoning | Not classified | Special | Not a normal voluntary effect choice; verify event semantics and event-value handling separately. |
| Wisp_Swap_Die | Pollen Theft | Specialized tactical projection | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |
| Wisp_Upgrade_Die | Overgrowth | Specialized tactical projection | Retain / verify | Existing deterministic/mean estimate or specialized projector is reasonable for now; verify activation-target alignment and double counting. |

## Implementation sequence (future steps, not performed here)

1. Build common deterministic `scoreConsequence(before, after)` adapters for exact dice, opponent modifications, Battle rows, purchase thresholds, and resource deltas; score increments once.
2. Prioritize multi-part Battle effects (Bloom Backflip, Gust, Transplant, Parting Thorn, Shift Happens, Vine Punishment), with top-level and target alignment tests.
3. Prioritize multi-draw and die replacement effects (Queen, Bursting, Reap, Root Recall, Root Awakening). Preserve available dice order and Battle capacity, use arithmetic means.
4. Close nested reuse effects (O Edelweiss, Vine and Again): first-effect consequences must be present when scoring the second.
5. Handle helper acquisition, theft and stacking (Bee-loved Bloom, Alluring Nectar, Whispering Wings); value concrete opponent denial separately.
6. Address medium items with shared adapters; preserve functioning fixed estimates when fully projected state changes would not materially improve decisions.
7. Run decision-pair tests and matched baseline-only traces before expert-versus-human experiments; audit old score bonuses for double counting.

## Limitations

This is a static code inspection, not an execution trace or empirical proof that the listed heuristics chose wrong. The existing 5B.8 matrix predates some of the newer 5B.5 sequential projection work; priority reflects the most recent archive and remaining limits, not literal earlier labels. Identifying 31 review candidates does not imply replacing 31 fixed scores: several share mechanisms, while many legitimate future estimates should remain heuristic.
