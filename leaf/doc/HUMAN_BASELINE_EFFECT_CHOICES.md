# Human Baseline — Effect Choices

## Status

**NOT CERTIFIED.** E-A inventory and E-B contract/gap mapping are complete. The next checkpoint is E-C, bounded implementation/test alignment.

Effect Choices is the final Human Baseline major area. It owns 20 `EffectStrategy` hooks. The rules engine remains responsible for which effects are active, phase legality, legal-choice construction, mutation, and mechanical randomness. Human Baseline chooses only among the legal choices supplied to it.

The difficult Battle realizations were already developed and protected during certified Battle work, especially B15. Effect Choices certification must reuse those analyzers rather than reopen or duplicate Battle strategy.

## E-A inventory conclusion

The 20 hooks are not 20 unfinished strategy systems. Certified Battle work already protects the difficult Battle-facing realizations, and several Cultivation choices already have direct behavior tests. The remaining work is principally to make the ordinary non-Battle/general contracts explicit, align a small number of generic heuristics, and prove all 20 hooks have a deliberate disposition.

## E-B approved cross-cutting contract

1. **Legality is engine-owned.** Human Baseline scores only supplied legal choices and never reconstructs hidden legality.
2. **Current visible consequence beats invented forecasting.** Use current state and the effect's immediate result. Do not search future turns, opponent response trees, hidden Wisp identities, or speculative combinations.
3. **Certified Battle analysis is reused.** A shared hook reached during Battle delegates to the existing certified Battle analyzer where one exists.
4. **Whole legal choices are compared.** Pair/subset/set hooks score the complete legal choice rather than greedily selecting members independently.
5. **Optional choices may decline** when the immediate visible result is not worthwhile under the hook's approved policy.
6. **Mechanical RNG is never sampled for hypothetical reasoning.** Use honest expectation before randomness; after actual randomness, use the fresh context supplied by the engine.
7. **Exact strategic ties use `StrategyRandomizer`.**
8. **No sophistication merely because the API permits it.** Human Baseline remains an ordinary reasonably competent player, not Tactical or Strategic play.

## Approved player-target threat heuristic

For `choosePlayer`, an ordinary Human Baseline player scans the table for the opponent who visibly looks most developed. The comparison is deliberately coarse and uses public development investment rather than VP or Wisp count.

For each legal opponent:

```text
visible development threat
    = total printed cost of all grafted Plant cards
    + extra dice power beyond the normal starting pool

extra dice power
    = max(0, total owned die sides - starting dice power)
```

`PlayerBoardView.dicePower` already totals owned die sides across Supply, Hand, Discard, Mulch, and pending Mulch. The shared Human Baseline development policy currently defines the normal starting pool as 3D4 + 3D6 = 30 die sides. Die purchase cost is its number of sides, so excess die sides are on the same simple cost-like scale as printed Plant cost.

This is intentionally an approximation of the human table scan: “whose Plant and dice pool look biggest?” It does not add VP, Wisp count, hidden information, current Battle position, card-combo strength, or forecasted scoring. Exact ties use `StrategyRandomizer`.

The current implementation instead scores opponent VP plus Wisp count. E-C must replace that old heuristic with this approved visible-development heuristic and add readable behavior-contract tests.

## Complete 20-hook contract/gap map

| # | Hook | Reach / effect family | Approved Human Baseline disposition | E-C status |
| --- | --- | --- | --- | --- |
| 1 | `chooseDie` | Broad shared die-target effects in Cultivation and Battle: raise/set/reroll/upgrade, Mulch, Root Cause-style transforms and related effects | Reuse existing phase/effect-aware scoring. Preserve certified Battle analyzers; preserve tested Cultivation Compost/Mulch/Sunlight and direct transform behavior. Compare immediate visible value only. | Audit all callers; fill only missing contract tests. |
| 2 | `chooseBattleDie` | Battle-only reroll/target mechanisms | Preserve certified expected current-Battle swing/need/disruption analysis. | Battle-protected; audit only. |
| 3 | `chooseRootWellBattle` | Root Well Battle branch | Preserve certified complete current-Battle realization. | Battle-protected; audit only. |
| 4 | `chooseCrossPlayerDieSwap` | same-size own/opponent Battle swap | Preserve certified Pollen Theft/current-Battle swap analysis. | Battle-protected; audit only. |
| 5 | `chooseOptionalDie` | Optional die targets; includes Cultivation branch of the “set die to 3 / reduce opposing row” effect and Wispquake-style protection where applicable | Use the existing effect-specific immediate-value policy. Optional means decline is legal. Do not invent future-Buy/future-Battle value. | Verify non-Battle reach and explicit decline contract. |
| 6 | `chooseDice` | Variable-size die subset, notably Root Recall | Score the complete legal subset. Preserve existing Root Recall behavior; no greedy member-by-member shortcut. | Existing Cultivation protection; caller audit. |
| 7 | `chooseDiePair` | Mandatory die-pair effects such as set-to-match and Battle Transplant Tulip pair | Score the complete ordered/legal pair according to the immediate effect. Preserve certified Battle pair analysis. | Audit non-Battle pair contract. |
| 8 | `chooseOptionalDiePair` | Optional pair/swap branch, including Transplant Tulip | Compare the complete pair with declining. Preserve certified Battle threshold/realization behavior. | Battle-protected; audit any non-Battle reach. |
| 9 | `chooseCritterAndDie` | Vine & Dine / trash Critter to raise die +5 | Prefer an immediate useful +5 while avoiding unnecessarily valuable/scarce Critter expenditure under existing policy. In Battle reuse certified tactical realization. | Add/confirm non-Battle behavior contract. |
| 10 | `choosePetalToDie4` | Gain D4 set to 4 vs trash D4/raise all +4 | Preserve existing phase-aware branch comparison. Battle uses certified tactical realization; Cultivation remains bounded immediate/resource reasoning. | Already strong; audit/certify. |
| 11 | `chooseBeeSource` | Bee-loved Bloom gain/steal source | Preserve gain/steal source policy; in Battle reuse certified immediate denial of visible Bee Support. Outside Battle, prefer taking an opponent Bee when legal rather than consuming the Grove source; no opponent future-tree reasoning. | Add explicit general contract coverage. |
| 12 | `chooseButterflyTarget` | Alluring Nectar / steal-or-gain Butterfly effects | Prefer a legal opponent face-up Butterfly when that obtains the resource from an opponent; otherwise use the best legal source under the existing simple policy. No future Butterfly-chain planning. | Add explicit general contract coverage. |
| 13 | `chooseOptionalPlant` | Optional own-Plant branch, including Cultivation Parting Thorn behavior | Compare the immediate benefit/cost of the legal Plant choice with declining. Preserve useful face-down Plants when flipping them up is beneficial and avoid sacrificing a valuable face-up Plant without sufficient immediate benefit. Do not add topology or future-refresh planning. | Genuine focused contract gap. Reconcile shared Plant valuation where appropriate without inventing a new scale. |
| 14 | `chooseOpponentPlantWound` | Shift Happens, Snip Happens, Battle Parting Thorn and related opponent Plant wound/flip targets | In Battle reuse certified immediate Plant-opportunity analysis. Outside Battle use visible general Plant-loss value among engine-supplied legal targets. Do not infer hidden value or optimize future Creature topology. | Audit general valuation/reuse after R3. |
| 15 | `choosePlantEffect` | Own Plant effect choice, including Shift Happens and Vine & Again | In Battle reuse certified current Plant opportunity. Outside Battle prefer the legal Plant whose visible current-phase effect is most useful under existing card scoring. No combo search. | Add explicit non-Battle contract coverage. |
| 16 | `chooseOEdelweiss` | O Edelweiss Play / Flip / Done branches | Preserve bounded branch scoring. In Battle reuse certified current Plant opportunity. Outside Battle compare the immediate visible Play/Flip consequence and allow Done; no two-step combo search beyond the card's required repeated resolution. | Audit non-Battle branch contract. |
| 17 | `chooseWispsToKeep` | Wisp Reckoning keep-limit choice | Enumerate exact legal keep sets and score the complete retained set. Do not infer hidden future draws or opponent plans. | Add explicit Human Baseline set-scoring contract coverage. |
| 18 | `chooseDieSize` | Gain any die to discard | Prefer the largest legal die size. No forecasting of exact future rolls or combos. | Simple direct contract test. |
| 19 | `choosePlayer` | Steal random Wisp from one opponent | Target the legal opponent with the greatest visible development threat: grafted Plant printed costs + die power above the normal starting pool. Do not use VP/Wisps as the threat score. | **Implementation change required.** |
| 20 | `chooseStrikeRow` | Battle row-target effects, including immediate Strike and row transforms | Preserve certified effect-specific Battle analyzers and row-need fallback. No new generic Battle redesign. | Battle-protected; complete caller audit. |

## Phase/caller ownership

The authoritative effect-to-decision-mechanism map remains `GameEffectDecisionRequirements.kt`; the handlers/special-effect classes construct the corresponding request and validate the returned legal choice. E certification does not move that responsibility into strategy code.

The main call sites are deliberately centralized:

- die and optional-die targeting: `EffectDieTargeting.kt`
- Battle die, Root Well, cross-player swap, and Strike Row targeting: `EffectBattleTargeting.kt`
- subset/pair draw and swap effects: `DrawEffectHandler.kt`
- die-pair value transforms: `DieValueEffectHandler.kt`
- gain-any-die and player/Wisp targeting: `ResourceEffectHandler.kt`
- card-specific branches: `VineAndDineEffect.kt`, `PetalToDie4Effect.kt`, `BeeLovedBloomEffect.kt`, `AlluringNectarEffect.kt`, `PartingThornEffect.kt`, `ShiftHappensEffect.kt`, `SnipHappensEffect.kt`, `VineAndAgainEffect.kt`, `OEdelweissEffect.kt`, and `WispReckoningEffect.kt`

The phase contract already records important splits such as Parting Thorn and Shift Happens using different mechanisms in Cultivation versus Battle, Petal to Die 4 adding Battle placement, Root Well being Battle-only, and cross-player same-size die swap being Battle-only. E-C must preserve those engine-owned phase distinctions.

## E-B gap classification

### Already protected; do not redesign

The certified Battle behavior behind `chooseBattleDie`, `chooseRootWellBattle`, `chooseCrossPlayerDieSwap`, Battle pair/optional-pair decisions, Battle Critter+die, Battle Petal to Die 4, Battle Bee source, Battle opponent Plant targeting, Battle Plant-effect selection, Battle O Edelweiss, and Battle Strike Row selection remains authoritative. B15's effect/mechanism audit is reused as evidence rather than repeated.

Existing Cultivation behavior-contract coverage for Sunlight/Mulch/Compost die selection, Root Cause-style die targeting, Root Recall complete-subset selection, and Petal to Die 4 should likewise be preserved unless the complete E audit exposes a direct contradiction.

### Focused gaps for E-C

E-C should concentrate on the uncovered general/non-Battle seams: optional die decline behavior where not already explicit; non-Battle pair behavior; Critter+die; Bee source; Butterfly target; optional Plant; general opponent Plant valuation; non-Battle Plant-effect choice; non-Battle O Edelweiss; Wisp keep sets; die-size choice; and the approved `choosePlayer` visible-development heuristic.

Shared valuation introduced by earlier certified areas should be reused where its meaning matches. In particular, E-C should inspect R3's Plant preservation abstraction before retaining duplicate Plant-loss arithmetic. It must not force reuse where “permanent loss,” “temporary flip,” and “current effect opportunity” are genuinely different concepts.

## No remaining E-B designer blocker

The player-target policy was the outstanding E-A design judgment. The approved visible-development rule above resolves it. The remaining E-C work is implementation/test alignment under already established Human Baseline principles; if source inspection exposes a genuinely new game-design choice rather than an implementation detail, stop and return it to the designer rather than choosing silently.

## Remaining checkpoints

- **E-C** — bounded implementation + readable behavior-contract tests for the identified gaps, grouped by coherent choice families.
- **E-D** — representative real-engine integration only where a meaningful seam is not already protected.
- **E-E** — explicit 20-for-20 hook audit: every hook has contract, implementation disposition, and adequate evidence.
- **E-F** — documentation/status consolidation.
- **E-G** — full `test integrationTest simulationCheck` regression.
- **E-H** — certify Effect Choices and close the eighth major Human Baseline area.

Effect Choices remains **NOT CERTIFIED** until E-H.
