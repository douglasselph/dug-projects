# Task 5 — Human Baseline audit of eight experimental Plant effects

Source snapshot: `leaf(20261022-202607).tgz`. Static source audit. No training/evaluation conducted.

## Scope and conclusions

The baseline has three distinct choices: purchasing a Plant, taking a Main action to activate it, and making the effect's subordinate targets/branches. These are NOT interchangeable. A recognized `GameEffect` in the executor does not mean the Human Baseline models its actual tactical value. `HumanBaselineCardScorerRegistry` registers card identities by stable Plant card name (Flower_14_04 / Flower_17_03 / Flower_11_03), and the purchase path uses these card-specific scorers. Thus experimental versions retain their base card identity and base valuation. This is consistent with comparable *identity* scoring, but it is not variant-sensitive purchase valuation.

### Per-variant coverage matrix

| ID | Effect | Buy valuation | Activation valuation | Subordinate choices |
|---|---|---|---|---|
| P1 | `GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_ONLY_THEN_CYCLE_D4S` | Petal card-name scorer (fixed) | +15 gain/steal; +7 per D4 already in Hand | Scores Grove 55; steal Hand 93; selects discard set using positive expected reroll gain |
| P2 | `GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_ONLY_THEN_CYCLE_D4S` | Same | Same, does not explicitly value immediate Hand availability | Same; legal Hand placement is enforced by effect handler |
| P3 | `GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S` | Same | Same, does not value expanded source opportunities | Steal Discard 81 vs steal Hand 93, both above Grove 55 |
| P4 | `GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S` | Same | Same, omits both Hand-arrival and extra sources | Same source preferences; legal Hand placement checked by handler |
| O1 | `PLAY_SPENT_OR_FLIP_ANOTHER_CARD_TWICE` | Edelweiss card-name scorer (fixed) | Cultivation: bonus up to 35 for face-down Plants. Battle: first-choice projection | Play face-down; Flip either; Done. Cultivation heuristic per candidate, Battle one-step `BattleOEdelweissAnalyzer` |
| O2 | `PLAY_ANY_OR_FLIP_ANOTHER_CARD_TWICE` | Same | Cultivation: **same bonus as O1**, undercounts face-up reuse. Battle first-choice projection includes face-up Play | Play either facing; Flip either; Done; Battle projection does not search second choice |
| S1 | `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW` | Snapdragon card-name scorer (fixed) | Generic initial +2 and row-need score; Battle enabled-Plant tactical analyzer supports complete same-die projection | Battle first die uses `BattleOwnDieCollateralAnalyzer` |
| S2 | `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW` | Same | Generic +2/row-need plus flat +8 flexibility bonus; **no equivalent enabled-Plant tactical projection** | First die uses simple Raise heuristic; second die pair falls through generic `PriorityScore(50)` instead of projecting two rows |

Scores above are from source, not measurements of real performance. The actual score is also shaped by competing actions, state, policy weights, and other modifiers.

## Decision-path evidence

- Purchase: `baseline/buy/PurchasePriority.kt` uses `cardScorers.forPlant(item.card).acquireScore(...)`; the card identity scorer definitions in `baseline/card/plant/flower/{PetalToDie4Baseline,OEdelweissBaseline,SappingSnapdragonBaseline}.kt` retain constant card-specific starting values.
- Activation: `baseline/card/CardScoringHelpers.kt` explicitly lists all four P variants, both O variants, and both S variants. Petal variants share the same gain/cycle adjustment; O variants share the same face-down count; S2 gets a flat +8 Battle flexibility bonus.
- Effect choices: `baseline/effect/HumanBaselineEffectStrategy.kt`: `choosePetalD4Source` uses static legal-source scores; `scoreDiceSet` covers all redesigned P variants; `chooseOEdelweiss` computes legal per-choice scores and delegates Battle scoring to `BattleOEdelweissAnalyzer`; `chooseDie` gives same-row S1 its collateral analysis; `scorePair` contains no S2-specific branch.
- Tactical projection: `baseline/battle/BattleOwnDieCollateralAnalyzer.kt` projects S1's complete opposing dice reduction but does not accept S2. `baseline/battle/BattleEnabledPlantAnalyzer.kt` similarly recognizes S1 in its deterministic collateral candidates and not S2. `BattleOEdelweissAnalyzer.kt` recognizes O2 face-up Play, but optimizes only a single immediate step.
- Legal choices (including unavailability and turn order) are established by effect handler/decision requests. An overly favorable generic score for a choice that is not legal does not make it selectable.

## Verified implementation correction in this Task 5 ZIP

`HumanBaselineCardScorerRegistry.forEffect` previously had no registration for any of the Petal/Edelweiss/Snapdragon research-only enum identities (even though by-name lookup still worked). Register the research identities against their existing corresponding Plant scorer. A regression test requires each registration. **This is a missing lookup registration, not a modification of preferred decisions or card costs.**

## Remaining research-critical representation gaps (not silently tuned in Task 5)

1. **S2 Battle targeting:** The pair-scoring fallback is identical (50) for substantively different anchor rows. This is a genuine tactical-feature gap, and the greatest immediate blocker to fair Human-vs-Expert interpretation. An accurate S2 projector must model first-die +2, independent second-die anchor, each opponent die's actual -2 capped at 1, and an accumulated raise to the second die capped at its sides, including both affected rows. It should be used consistently for top-level Plant activation AND effect target selection. Do not substitute a larger arbitrary flexibility bonus.
2. **P1–P4 valuation:** These codes share a card activation adjustment even though Hand vs Discard changes same-turn tactical benefit, and stealing from Discard expands legal availability. For P2/P4, acquisition to Hand may be valuable for the subsequent optional D4 cycling and immediate Battle placements. Eligibility remains correctly differentiated, but *forecast value* is not. Candidate-specific source scores also impose strong Hand-steal-over-Discard-steal preference unrelated to the exact effect value. A faithful valuation needs forward projection, not a blanket point increase.
3. **O2 Cultivation:** Top-level valuation counts only face-down cards, though O2 can play face-up cards. Its downstream `scoreOEdelweissFallback` can score face-up Play, so the selection can be sensible once activated even when the activation heuristic undervalues taking the action. Battle top-level evaluation already recognizes face-up eligibility but its projection remains deliberately one-step rather than full two-step.
4. **Purchase:** Card-name-based purchase valuation is the same for variants with the same title/cost. This avoids artificial economic favoritism, but it cannot by itself predict that one experimental rules version is more usable. Until expected play value is projected through purchasing, expert-vs-Human purchase comparisons have a known baseline limitation.

These gaps should be corrected through a separate focused *baseline tactical valuation* implementation and tests **before** drawing variant-strength conclusions. Their presence is not proof that any real effect is too weak or too strong. The current Task 5 ZIP deliberately does not rewrite scoring numbers without a validated state projector.

## Suggested verification

`./gradlew test --tests '*HumanBaselineCardScorerRegistryTest'`

Then `./gradlew test integrationTest simulationTest`.

The task was audited statically; no local Gradle test success is claimed.
