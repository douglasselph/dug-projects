# A2 — Forced matched-state Root Four More effect calibration (v38)

**Status (November 13, 2026):** Code and deterministic tests prepared. **Kotlin test and direct-Java experiment have NOT been executed in this editing environment** (Gradle 8.13 is unavailable offline). No outcome or game-balance verdict yet.

## Research question

The previous 8-game frozen-policy intervention could not mechanically test the suppressed Root Four More effect because the policy chose not to activate an inert Plant. This new *mechanical diagnostic*, separate from policy and Buy search, **forces** an actual activation in an otherwise identical local Battle state, under three target-seat-only effect definitions:

1. `RAISE_DIE_PLUS_4` — official effect.
2. `RESEARCH_NO_EFFECT` — deliberately inert, but **executed**.
3. `RESEARCH_RAISE_DIE_PLUS_20` — exaggerated Raise, subject to D20 cap.

The canonical `Root_05_02` definition is loaded from the current CSV. The experiment creates three fresh Games for each starting state, each with the same physical legal nine-stack Grove (`FirstGameDefault`), the same two player dice Hands, placements, one face-up Root Four More, and the same 3/2/2 Round deck structure. It uses the current `THREE_ENGAGEMENTS` Battle structure and opens its top Strike. The *actor's actual first D20* is the fixed legal target; no AI may avoid or retarget the forced resolution. The source Plant is flipped face down after use. Other seats never receive the experimental effect.

These are **synthetic legal local Battle states**; they are not drawn from a naturally played ten-round game or claims of whole-game reachable-state frequencies. The intervention replaces one immediate Plant activation only. It does not model alternative Main Actions forgone, full Engagement sequencing, Doom, card purchase costs or future development. Full-game causal results remain separate.

## Scenario matrix

| Scenario | Target D20 | Opponent D20 | Question |
|---|---:|---:|---|
| `raise4_turns_loss_to_win` | 2 | 5 | Does the ordinary Raise +4 actually turn a losing Strike into a win? |
| `only_raise20_turns_loss_to_win` | 2 | 9 | Does the exaggeration create a tactical win that Raise +4 does not? |
| `raise20_forces_tie` | 2 | 20 | Are tied top totals correctly distinguished from a win? |
| `capped_raises_no_value` | 20 | 19 | Is a capped Raise correctly recorded as zero, even when the actor already wins? |

Each player also places lower dice into the Middle and Bottom Strikes so the initial Battle placement is physically valid. The opponent has a grafted face-up Root, so Wounds have a legal Plant target. No dice are rerolled, so these comparisons do not depend on RNG coupling.

**Expected deterministic outcome checks, not observed results:** With own D20=2 and opponent=5, suppressed loses and official Raise +4 wins. Against opponent=9, official Raise +4 still loses but Raise +20 wins. Against opponent=20, the exaggerated Raise creates a tie (no winner), not a win. With the target already showing 20, every condition has zero extra pips, proving physical die caps are enforced.
 For each scenario, all three conditions must have the identical saved *pre-action* snapshot.

Each actual effect execution uses `DefaultGameEffectExecutor` and the existing `recordResearchEffectOutputs` seam. The top Strike is then resolved with **the real `StrikeResolver`**, including normal VP, tied-leader handling and Wound candidates. Resulting outcome measurements are **mechanical counterfactuals**, not estimates of game win probabilities.

## Execution on Ubuntu, from the `leaf/` directory

```bash
python3 bin/verify_v38_forced_root_four_more.py --plan
python3 -m unittest bin.tests.test_v38_forced_root_four_more -v
python3 bin/verify_v38_forced_root_four_more.py --run
```

The `--run` launcher runs a **single Gradle invocation** for the focused Kotlin regression tests and simulation runtime classpath, then starts **one direct-Java JVM** for all twelve cases. No repeated Gradle invocation per condition and no training/research checkpoint modifications. It verifies the exact TSV schema and numerical effect/Strike invariants. It produces one `.tgz` in `~/Downloads`, prints `UPLOAD THIS FILE:`, and retains detailed logs/TSV under `output/experiments/v38-forced-root-four-more/`. Failure also creates an archive with diagnosis; do **not** interpret a failure as a card-balance finding.

### Required acceptance gate

- All 12 conditions execute the intended Root Four More effect (including the `RESEARCH_NO_EFFECT` handler).
- Suppressed effect resolves with **zero** realized pips; official and exaggerated effects record exact actual pips, limited by D20.
- Same-state official activation can reverse a loss; the amplified effect reverses at least one additional loss; full-cap state realizes no additional pips.
- Actual top-Strike winner, VP and Wound data correspond to `StrikeResolver`, including no winner on all-player ties.
- Opponent keeps the official effect identity and the canonical CSVs remain unchanged.

**Only if this gate passes** do we claim forced **local mechanical sensitivity**. The next A2 work remains a larger, independent held-out whole-game study, meaningful non-VP amplification under capable policies, and deliberately trained counterstrategies. No 36-Plant balance certification results from this single fixed-state diagnostic.
