# C4 — Replay equivalence specification

Status: **specification only** (2026-10-12). No simulation, rules, policy, or RNG implementation is changed by this document. C5 will implement and validate diagnostics. The guarantees below distinguish an intended contract from what the current implementation has actually demonstrated.

## Terminology and inputs

A **run** is a newly constructed game with a specified code/rules version, game content, player count and seating, Round setup and overrides, policy factories and model weights, mechanical seed (`GameConfig.seed`), and strategy seed (`GameConfig.strategySeed`). Reproduce all of these inputs, not just one seed. A **pair** consists of a control run and a comparison run. A **fork** changes a specified decision or policy while starting from equivalent initial inputs. The current `CultivationDecisionReplay` names Main decision consultations with `(roundNumber, playerId, consultationIndex)`; this is a lookup ordinal, not a persistent game-event identity.

A **matching logical event** is an event that shares the documented *event key* between histories. Matching event keys do not imply identical game states or equivalent consequences. An event existing in just one history is **unmatched**, not a failed replay match. Distinguish (1) unchanged game outcome, (2) unchanged random input, and (3) equivalent underlying random percentile: they are different assertions.

## Three levels

| Level | Inputs | Intended equivalence | Current status and limits |
| --- | --- | --- | --- |
| **Exact replay** | Identical complete configuration, policies, seeds, implementation, and deterministic external data | Identical ordered decisions, game-mechanical events, acquisitions, Battles, and final scoring | Existing seeded construction and decision tracing provide infrastructure; **a complete-game byte/semantic-equivalence guarantee is not yet established by C4**. Verify in C5. Avoid comparing volatile diagnostics such as timestamps or object addresses. |
| **Seed-matched counterfactual** | Same base config and mechanical seed, with an explicit policy/decision intervention | Initial conditions align. Independent strategy/mechanics RNG streams mean sampling strategy scores does not consume mechanical random values. Other events **may** diverge once their call order changes. | C1 provides per-player streams for decision RNG and score-range RNG. In current `CompostCounterfactualRunner`, `eventBasedDieRolls` is **not** set, so its replays use the default sequential mechanical rolls. Same seed is not a guarantee of later matching dice. |
| **Event-matched counterfactual** | Same mechanical seed, opt-in event-based die rolls, plus an explicit intervention | For covered die-roll events with equal `(round, player, stable die ID, occurrence)`, the underlying event randomness is invariant to unrelated extra/missing RNG calls. In shared-percentile mode the same `u` is used for different die sizes, but the resulting face may change. | C2/C3 support selected roll paths through `EventRollRandomness` when `GameConfig(eventBasedDieRolls=true, sharedPercentileDieRolls=true, seed=...)` is set. Not all mechanics or rolls are event keyed. Full event matching is not yet provided. |

`eventBasedDieRolls=false` and `sharedPercentileDieRolls=false` are the defaults. The second flag requires the first. Event matching is **not automatically active** in existing saved experiment outputs or the current `CompostCounterfactualRunner`.

## Event-key contract for covered die rolls

Current key: `(mechanical seed, round number, player ID, player-local stable physical die ID, roll occurrence for that die within that round)`. The occurrence counter resets by virtue of round being part of its key and increments only for that die's recorded event. Upgrades transfer the stable roll identity to the replacement die in event-based mode. Newly acquired dice obtain IDs allocated within the particular history and therefore may not match after branching. Different timing, owner, or occurrence yields a different key even for the same physical die.

**C2 (event-face mode):** deterministic face is computed from the event key *and sides*. Same key and same die size => same face. **C3 (shared-percentile mode):** compute a stable `u` in `[0,1)` from the key without sides, then `face=floor(u*sides)+1`. Same key with D4 in control and D6 in comparison => same `u`, **not** necessarily the same rolled number. Each size retains its own uniform marginal face distribution. This is intentional coupling for comparative research, not a physical game rule.

Do not infer roll matches from position in a Chronicle alone. An ordinary draw that occurs in only one branch must not shift other dice's event-keyed results, but it can alter subsequent decisions and the presence, timing, or count of future events.

## Legitimate divergences after an intervention

The earliest changed Main decision is the **intervention divergence**. Subsequent differences are expected, not replay defects, when they follow from legal state changes: a die held versus discarded; upgrades changing die size or draw timing; differing legal Buy options, purchasing order, acquired dice and Plants; extra or skipped roll occurrences; token and Wisp changes; Battle placements and scores; changed game lengths or effect triggers. Game-state and action differences can propagate even while certain matching random inputs are preserved.

**Unexpected divergence candidates** needing investigation: a different result for an identical covered roll key at the same die size and mode; a different `u` for an identical key in shared-percentile mode; one player's score-range random stream changing solely because another player evaluated extra ranges; a roll event disappearing without a preceding intervention or state divergence; or two completely identical runs yielding semantically different traces. These are diagnostics, not proof of defects until all config and keys are verified.

## C5 diagnostic requirements (proposed; NOT yet emitted)

Every paired report should capture:

1. **Provenance** — source revision/commit, rules and content hashes, Round/Plant overrides, player count, seating and policy/weight file hashes, mechanical/strategy seeds, flags (`eventBasedDieRolls`, `sharedPercentileDieRolls`), fork specification, and schema version.
2. **First divergence** — first changed decision consultation (policy preference versus enacted choice), first differing material game-state event, and first RNG-key mismatch, with round/player/event context. Report these separately; one may be absent.
3. **Covered roll ledger** — run side, monotonically ordered local event ordinal, round, player, die ID, occurrence, sides, underlying percentile (`u`, if enabled), resulting face, and event origin (draw, reroll, effect) where known.
4. **Roll matching** — intersection and differences of event-key sets; matched same-size identical face count, matched cross-size shared-percentile count, unexpected mismatches, control-only events, comparison-only events. Also report coverage: all observed roll events versus number actually using C2 keys; do not quietly count uncovered events as matched.
5. **Decision matching** — legal alternatives, learned preferred action (where available), actual action, reason for intervention, score breakdown including sampled range influences and external weights (where available); count unchanged, changed, and unmatched consultations.
6. **Game outcomes** — final VP and component deltas for **both** players, VP margin, winning share, dice inventory/count/side-power, purchases, and relevant Battle results. Keep selected full Chronicles, marking control-only/comparison-only events.

Suggested outputs: `pair-manifest.json`, `decision-diff.tsv`, `roll-events.tsv`, `roll-match-summary.tsv`, `divergence-summary.txt`, and optional complete `control-chronicle.txt` / `comparison-chronicle.txt`. Schemas and exact field names are proposals for C5, not current file formats.

## C5 acceptance checks

- Repeat identical configuration and seeds with no fork: semantically identical decisions, event ledger, purchases, and final summaries across multiple runs and both RNG modes.
- Add an extra *score-range sampling* call for Player A: Player B's score-range and decision RNG streams, and the mechanical stream, remain unaffected at matching points. Note: altered *actions* can legitimately change event existence and state.
- Introduce a counterfactual Draw-versus-Compost choice: identical covered die-event keys keep the same face at equal sides, and the same `u` at different sides in shared-percentile mode; unmatched events are reported, not forced to match.
- Upgrade a D4 to D6 in one branch with matching player/round/die ID/occurrence: verify `face=floor(u*sides)+1` for each size and preserved `u`.
- Confirm valid face ranges, per-size uniformity over many keys, and that same seeds do not hide changed content or weights.
- Add tests for missing fork target, divergent roll occurrence, newly purchased dice, and partial RNG coverage; ensure each is reported accurately.

## Important interpretation limits

Seed matching and shared percentiles are **variance-reduction devices**, not proofs of causality in a single game. A changed action can genuinely alter the future opportunity structure, and unkeyed mechanics may still diverge with call order. For Compost balancing, summarize distributions of VP and win-share differences across many seeds and positions; interpret narrated games as examples of observed paths, not as proof that one decision caused every later scoring change.

## Implementation boundaries

C4 adds **documentation only**. It intentionally does not change `GameConfig`, die IDs, die-roll algorithms, replay runners, game rules, or saved experiments. C5 is responsible for implementing the ledgers, comparison utilities, coverage accounting, and automated validation before claiming these guarantees are fully verified.
