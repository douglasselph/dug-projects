# Battle pacing A/B — v37.03 repository audit and first implementation plan

Status: experiment design / source inspection, 2026-10-30. Candidate B is **not adopted**. This document changes no game rule, simulator mechanic, policy, card data, or research weight.

## Authoritative inputs

- `Leaf_and_Let_Die_v37-03(1).pdf`, especially printed pages 14–18, 20–25.
- `Battle Pacing & Balance Experiment.docx`, Candidate A / B specification.
- Source archive `leaf(20261030-184541).tgz`, including `doc/` and the source under `src/`.
- `doc/SIMULATION_OPERATIONAL_READINESS.md`: operational gate, not a balance certificate.
- `doc/A03_SCOPE_BOUNDARIES.md` and `doc/A03_FEATURE_VERIFICATION_WORKFLOW.md`: protected-source review requirements.

## Candidate comparison

| Axis | A: v37.03 control | B: three engagements |
|---|---|---|
| Opening | Reveal, Draw/Roll 3, Rank/Place | Identical |
| Normal Main | Exactly 2, First mandatory before Supports; Final exits | At most 1 per Engagement; max 2 total |
| Sunlight | Support, bonus Main, neither spends token nor exits | Consumes one of up to 2 Support slots for that Engagement; extra Main does not consume normal token |
| Support | Repeated turns until Final Main | Up to 2 per player per Engagement, including Sunlight |
| Actor order | Left-to-right Battle Grid columns | Per-Strike highest currently showing die starts, tied by leftmost Grid column; then physical clockwise seating |
| Resolving | Open rows after all players finish | Immediately after each Engagement, High/Mid/Low |
| Wounds | At final resolution | Immediately after each engagement |
| Row lock | Only special Wisp closure | A resolved Strike cannot subsequently be modified; retain ordinary resolved dice/Critters through Doom |
| Doom | All remaining placed dice after Resolve | Once after Low, including ordinary locked rows |

The three-Strike Battle Glade and the v37.03 VP/Wound semantics are preserved. The standard current Round Deck is **3/2/2** (printed rulebook page 5). **Do not** change global Main token count to 3.

## Source-based reuse map

- `src/main/kotlin/dugsolutions/leaf/v35/game/round/battle/BattleRound.kt` orchestrates opening Draw, Rank/Place, existing action loop, all open Strike resolution, Doom, Cleanup. The public step seams are suitable for integration fixtures.
- `.../game/round/battle/BattleActionCoordinator.kt` owns production legal Main/Support generation, policy handoff, action execution, Sunlight, Wisp policy filtering, and Chronicle entries. Its current lifecycle is specifically FIRST/FINAL and cannot just be repeated per row.
- `.../battle/BattleState.kt` holds ordered players and player-level Done; for Candidate B, track remaining normal Main tokens and per-Engagement action allowances separately. The existing Done semantics must not mistakenly prevent third-Engagement Supports.
- `.../battle/domain/BattleGrid.kt` holds row closure, withdrawals, dice, Critters, and capacity. Normal placement checks closed rows. Direct die swap/replacement operations require lock audit.
- `.../battle/BattlePlacementResolver.kt` screens open rows and capacity when placing newly Drawn dice.
- `.../battle/StrikeResolver.kt` already resolves a single row with real VP/Wound rules. Reuse it.
- `.../battle/DoomResolver.kt` already uses all remaining Grid placements and must still run once after all engagements.
- `.../effect/special/WispLastWordEffect.kt` already resolves, clears, and closes a row. It is not equivalent to ordinary B resolution (which retains dice).
- `.../effect/special/WispquakeEffect.kt` currently rerolls all Hand dice and does not filter closed Battle rows.
- `.../chronicle/` emits typed events, scoped Main/Support/Strike effects and detailed logs; add **experiment-specific** actor-window/engagement reports, not human-time claims.
- `.../game/di/GameFactory.kt` constructs fresh seeded game graphs; `.../game/round/RoundCoordinator.kt` exposes `revealNext/executeRevealed`; `.../game/GameRunner.kt` currently runs whole games. Prefer replaying identical prefixes to inventing a mutable Game clone.
- `src/simulation/` has seeded, separate-conditions and replay research code. `doc/archive/research-infrastructure/PHASE_C5_REPLAY_VALIDATION.md` describes event-keyed shared-percentile dice, with limitations for Wisp shuffles/identity divergence.
- `src/test/`, `src/integration/`, `src/simulationTest/` provide the unit/integration/simulation layers.

## Fidelity audit: must resolve or stratify before serious A/B inference

1. **D4 Doom mismatch**: v37.03 printed page 18 and glossary page 25 return Trashed D4s to Graft Bed. `TrashResolver.kt` and `DoomResolver.kt` explicitly remove them from play. This is a *pre-existing* discrepancy; do not fix silently in the scheduler patch. Because it affects later games, resolve it before interpreting multi-Battle outcomes.
2. **Wisp's Resolve / Last Word**: printed Wisp CSV says resolve a Strike now, then return all dice/Critters in that row. The existing handler clears and closes. Candidate B ordinary row resolution must *not* clear. Decide whether the Wisp may target future open Strikes, and what happens if it resolves the active Strike before all players act.
3. **Wispquake**: existing handler targets every player's `dice.hand`; locked rows are still Hand dice. Define how Wispquake handles previously locked rows without retroactively changing resolved outcomes. Record/exclude unresolved cases from pilot inference until a ruling is approved.
4. **Cross-row swaps/replacements**: Pollen Theft and Transplant Tulip. The core Grid swap/replace methods do not themselves screen closed rows; action/effect legal-choice generators need explicit review for all swap and replacement branches.
5. **Empty Strike squares**: initial Draw can yield fewer than 3 dice. `Reap What You Roll`, Root & Scoot, and Wisp clearing can also create emptiness. The candidate starter's row highest is provisionally 0 for an empty square; ties use Grid column index.
6. **Multi-row card buffs/debuffs**: Root Appreciation, Bee-loved Bloom, Rise and Vine, Saplink Trellis, Bloom Backbone, and any effect changing already placed dice/committed Critter value. Effects should not retroactively modify locked outcomes. Test whether underlying targeted effects restrict locked dice.
7. **Immediate wounds and later Refresh**: `WoundResolver` applies Flip-or-Snip, including choices; earlier Wounds affect later card access. Water Refresh and Worm Flip may restore cards on subsequent turns. Explicitly track card availability after each Engagement.
8. **Timing and action chains**: Candidate B allows one player window with up to one ordinary Main and up to two Supports; Sunlight is one Support that yields an extra Main. Test all feasible sequences without reusing the baseline Final-Main exit semantics; do not assume an action exists when all legal actions are exhausted.
9. **Future-row steering**: moves/Raises before a later engagement may change its starting player. Ordering for the current Engagement is frozen once chosen.
10. **Round Effects / scarcity**: record when shared supply is empty; Battle Round Effects may change Plant/resources, eligibility and future Engagement decisions.

## Smallest first code increment (not implemented by this document)

Build an **experimental-only immutable Engagement-order selector**, initially separate from `BattleActionCoordinator`:

```
selectOrder(
    battleState: BattleState,
    physicalSeatOrder: List<PlayerId>, // Game.players order
    row: StrikeRow
) -> EngagementOrder(
    row,
    startingPlayerId,
    playerOrder,
    highestDieByPlayer,
    battleColumnByPlayer
)
```

Contract:

- Reject already closed rows and non-permutations of the participating seats.
- For each participating player, inspect their *currently placed dice in that row*, not whole column or Dice Supply; maximum is 0 if none.
- Choose highest max; ties resolved by **leftmost Battle Grid column**, not another die.
- Rotate clockwise **physical seat** order to start with that player (not left-to-right Grid order).
- Freeze the returned `EngagementOrder` and log evidence; mutating dice during a window does not mutate this order.
- Consume no RNG and change no production state.

Unit tests (2/3/4 players): highest row die, equal highest but different second die, leftmost column tiebreak, non-Grid seat order, empty squares, future-row die changed between engagements, order frozen after selection, invalid/duplicate seats and closed row. Build cases using `BattleTestFixture` / legal `BattleState` placements, not arbitrary off-board dice.

**A03:** new production/simulation source is protected by default. Run `python3 bin/a03_verify_boundaries.py` and follow `doc/A03_FEATURE_VERIFICATION_WORKFLOW.md` for explicit path-by-path review; do not silently rewrite the protected manifest. Doc and test-only modifications can precede protected-source approval.

## Next: first actual paired A/B battle

1. Instantiate the same `GameFactory(GameConfig(...))` input twice with a fixed seed, player policy factories, fixed Round Card schedule, identical Grove, and the existing event-based/shared-percentile die-roll configuration where applicable.
2. For a Battle at round N, replay **all preceding rounds under A in both conditions**. Assert a complete pre-Battle fingerprint: player IDs/physical seats, scores, Plant state/positions, resources, dice zones, Wisp identities as permitted for reproducibility audit, Round Card, Grove, and initially placed dice after Draw/Place.
3. Run A with the unchanged `BattleActionCoordinator` and normal `executeStrikes`.
4. Run B with a separate experimental coordinator invoking the same real legal-action/effect/Strike/Doom resolvers. Keep the two-normal-Main-token budget across all three rows and per-row 2-Support allowance; resolve+lock after every row.
5. Emit per-action/Engagement logs including initial highest row die, absolute seat, Grid column, starter, actor index, legal-action count, chosen action, changes to all unresolved row totals, normal/Sunlight Main counts, Support counts, pass/no-op, VP/Wounds, row closures, Doom, final scores, and reasons for unhandled effects.
6. Re-run A against A to confirm exact replay, then a few A/B seeds. Reject any trial that violates a known blocker instead of treating its output as balance evidence.

For **middle/final** Battles, identical prefixes mean that preceding Battles follow control A in both conditions, so the estimate is the *local intervention at the chosen Battle*, not an entire alternative-B history. For whole-game B adoption impact, run a separate experiment that applies B to all Battle Rounds, accepting naturally divergent state histories.

## Experimental design when the full B rules are verified

- Correctness gate: deterministic miniature cases + approximately 1,000 valid smoke battles, covering 2/3/4 players, special cards, empty/full squares, ties, Wounds, Doom and locks.
- Balance: >=10,000 paired valid battles per major cohort as feasible, including early/middle/final under 3/2/2, asymmetry of 1–2 vs 8–12 available Supports, stronger/weaker Plant engines, score lead/trail, seats, and policies.
- Policies: Mechanical Control only for legality, Human Baseline/fast tactical for preliminary balance, and bounded lookahead where implementable. Do not reopen the C04 preference-calibration backlog as a prerequisite.
- First-mover/last-mover: report raw unique/shared wins, VP, action position, pre-turn row totals, probability adjusted for initial die/resources/seat, tactical last-action swing, and counterfactual order swaps **in a separately labeled noncanonical research cohort**.
- Pacing: per-window handoffs, distinct actions, duration proxies for physical work/decision complexity/reassessment, longest inactive span and terminal waiting after the last meaningful move. Publish low/base/high assumption bands, **not observed minutes**.
- Report exact valid/attempted N, exclusion reasons, confidence intervals, seeds, CSV/JSON schema, code/data fingerprints, policy composition and CPU throughput separately from model-based human time.

## Local verification of supplied archive (this review)

- `python3 bin/a03_verify_boundaries.py` — **PASS**, 480 protected files, 0 protected changes (the extracted archive before adding this documentation).
- Gradle 8.13 executed `help` successfully from the supplied `leaf-gradle-portable.tar.gz` cache.
- Focused `test` build was attempted but **NOT RUN**: offline Gradle could not resolve Compose, Koin, Kotlin and Coroutines JARs missing from the portable cache. No Kotlin regression or A/B experiment was executed in this review.
- User-reported operational readiness on 2026-10-30 is accepted as project context, not independently reproduced in this container.
