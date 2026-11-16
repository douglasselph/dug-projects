# Immediate expected pips and Roll Reward value

**Implementation slice:** Shared immediate die/roll consequences, before any future-round forecasting or resource personalities.

**Why this exists:** Human Baseline currently compares unrelated priority scores for Draw, Round and Plant Build actions. The shared numerical facts must be available before those scorers can be migrated. This step provides *facts*, not a new Build-action chooser.

## Plain-English model

1. **A newly rolled die has an expected face value:** D4 = 2.5, D6 = 3.5, D8 = 4.5, D10 = 5.5, D12 = 6.5, D20 = 10.5. This is the contribution to the current Hand's potential spending pips; it is *not* the die's maximum face.
2. **An eligible normal roll may provide an additional strategic reward.** A 1 awards a choice of available Critter; a 2 draws a Wisp. Our *provisional* value of a Critter option is +2 pip-equivalent units and of a Wisp option +3. They have probability `1 / number of die sides` apiece. Thus a D10 with both rewards available has 5.5 expected spending pips **plus** 0.2 Critter option value and 0.3 Wisp option value, for **6.0 strategic units**.
3. **Reward availability matters.** No Critter option value is credited when the Grove has neither Bees nor Worms. No Wisp option value is credited when the deck is empty or the player has reached the provisional five-Wisp strategic reserve limit. This is a valuation assumption, not a claim that the underlying mutable `WispHand` enforces a hard cap. Immediate-play Wisps and individual Critter options are not modeled here.
4. **A Raise isn't a roll.** Raising a D20 showing 18 by 4 contributes **2 real current pips**, not 4, and does not create Critter/Wisp Roll Rewards. Flip, set-to-max, and other deterministic face changes likewise receive no speculative rewards. Unsupported effects return `null`, rather than silently receiving a value of zero.
5. **A reroll or Upgrade-and-use-now replaces an existing result.** Subtract the die's *currently showing* pips from the fresh expected face. If the new roll awards normal Roll Rewards, add those new options. We do not claw back a reward already earned from the original roll. A reroll can have a **negative** expected spending-pip change.
6. **An ordinary Draw adds a die, not a replacement.** Multiple draws use actual lowest-sided-first Supply/Discard cycling via `ExpectedDrawProjection`; the count never exceeds available dice. Each prospective normal roll gets a provisional reward expectation using the given availability snapshot. This is an expectation, **not** a prediction that the same limited physical reward will remain available after every individual draw.
7. **Roll Reward policies matter.** The current `RollResolver` permits normal roll rewards in both Cultivation and Battle. Explicit `IGNORE` produces no reward; `DEFER` is counted as zero *until the roll is committed*. The caller must supply the actual roll policy; face modifications must not be misclassified as new rolls.

## Code and types

- `ImmediatePipValuation` provides pure arithmetic for `roll`, `draws`, `reroll`, `upgradeUseNow`, `raise` and the deterministic effects supported by `SharedDieEffectOutcome.faceEffect`.
- `ImmediatePipValue.spendingPips` is current-round, actual expected **face value**. `critterOptionValue` and `wispOptionValue` are **strategic equivalents**, not dice available for spending or Strike placement. `pipEquivalentTotal` is their sum; it must never be substituted for physical buying power.
- `RollRewardOpportunities` supplies availability and the two provisional reward equivalents. `fromVisibleContext` uses only public visible information; never reads hidden Wisp identities or future rolls.
- Existing `CultivationPipOutcome` methods deliberately continue to return *face-only* means for their legacy callers. No old priorities have been retuned or new personality enabled.

## Acceptance tests

`./gradlew test --tests '*ImmediatePipValuationTest'`

The focused suite checks D10 = 5.5 / 6.0, D4 = 2.5 / 3.75, capped Raise +4, no rewards for face changes, signed rerolls, ignored/deferred rewards, available versus unavailable Critters/Wisps, physical die support and Supply/Discard Draw order. This test is a **mathematical contract**, not a strategic or competitive certification.

## Relationship to subsequent work

- **Future dice forecast:** strictly future rounds, per-die encounters, 3/4/5 Cultivation Draw scenarios and three-die Battle openings. Never count the current Draw again in the future forecast.
- **Common Build comparison:** combine current action consequence, separate future value and any additional threshold/VP consequences with named components and no double counting. Only at this later step should Draw and simple Plant activations start using the shared value to rank legal actions.
- **Resource personalities:** Water/Sunlight/Mulch are fixed, descriptive human preferences and should not be learned by optimizing Human Baseline win share. Mathematical facts from this helper are shared among personalities.

## Previous Build snapshot: compile repair

The first Ubuntu capture archive `leaf-human-baseline-build-20261116-210640.tgz` reports `FAIL_INCOMPLETE`: compilation of `BuildBehaviorSnapshotMain.kt` failed because it referenced the nonexistent `CardDataFiles.WISP_CARD_LIST`. The correct existing filename constant is `CardDataFiles.WISP_LIST`. This changeset fixes that reference. **No baseline game or snapshot archive was produced by the failed run.** Rerun `bin/capture_human_baseline_build` and retain its successfully completed archive before modifying legacy Build selection behavior.
