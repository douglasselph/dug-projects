# Plant Effect Variant Contracts — Task 1

Status: **specification only** (2026-10-22). No production enum, handler, baseline policy, canonical CSV, or simulation behavior is changed by this document.

Source of truth reviewed: `leaf(20261022-193120).tgz`, especially `GameEffect.kt`, `PetalToDie4Effect.kt`, `OEdelweissEffect.kt`, `DieValueEffectHandler.kt`, `EffectStrategy.kt`, and `data/research/resync/resync-current.csv`.

## Experiment identity and invariants

Use stable short experiment identifiers `PETAL-P1` through `PETAL-P4`, `EDEL-O1` through `EDEL-O2`, and `SNAP-S1` through `SNAP-S2`; never reuse an ID with different semantics. Keep one shared card/rule definition across all player counts. In any comparison, change only the effect code for the card under test, without changing card type/cost, scoring, or the other cards.

Petal To Die 4 stays Flower 14 and scores `PER_OWNED_D4` (one VP per D4 still owned at end game). All Petal versions finish with: optionally choose zero or more D4s currently in **your** Dice Hand, move them to your Dice Discard Bin, then perform that many normal Draws, with Roll Rewards. This is a *discard*, not Trash; the newly acquired D4 participates in this step iff it is in your Hand at the start of this step. D4s gained or stolen are **not rolled**, so acquisition never triggers Roll Rewards. Supply-first, refill-as-needed normal Draw rules apply. If both Supply and Discard are empty when drawing, that Draw yields no die; continue remaining attempted Draws normally. In Battle, discard of placed dice also removes them from their Strike Squares; newly drawn dice must use normal legal Battle placement, and any drawn die with no legal slot goes to the owner's Discard (matching existing redesigned handler).

### Petal source/destination matrix

| ID | Proposed stable GameEffect constant | Acquisition destination | Steal sources |
| --- | --- | --- | --- |
| PETAL-P1 | `GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_ONLY_THEN_CYCLE_D4S` | Your Discard | Opponent Hand only |
| PETAL-P2 | `GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_ONLY_THEN_CYCLE_D4S` | Your Hand | Opponent Hand only |
| PETAL-P3 | `GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S` | Your Discard | Opponent Hand or Discard |
| PETAL-P4 | `GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S` | Your Hand | Opponent Hand or Discard |

`PETAL-P1` is the current research implementation, presently named `GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW`. The new constants are proposals, not present enum members. Retain mapping of the historical name to P1 for compatibility unless deliberately migrated.

**Petal legality and atomicity (proposed for P2–P4, consistent with P1 where already implemented):**

1. **Gain** may target only a D4 actually available in the shared Graft Bed; it consumes that supply. **Steal** transfers a single existing physical D4 from one *other* player's permitted zone and changes ownership. It does not consume a Graft Bed die. Never steal from a player's Supply, Mulch, or another zone; discard-source variants authorize Dice Discard Bin in addition to Hand, not Supply. No random opponent/zone selection: the decision policy chooses one currently legal source and exact die. For hand sources in Battle, remove the stolen die from its owner's Battle Strike Square and Hand; for discard sources remove it from the opponent's Discard only. Preserve a stolen die's current face value; no roll or Roll Reward.
2. If the Graft Bed has no D4, **Gain** is not offered, but Steal may still be offered. If no opponent has an eligible D4, Steal is not offered, but Gain may still be offered. If neither source exists, the effect is not executable and no discard/redraw suffix may be invoked. A source that becomes stale is rejected rather than replaced silently.
3. For **to-Discard** versions, place the acquired die directly in your Discard. It cannot be selected among the D4s discarded from Hand in the suffix. Its value may be drawn later if normal refill permits; this is intentional.
4. For **to-Hand** versions, place acquired die directly in your Hand without rolling. In Cultivation it is immediately selectable for the suffix and may therefore be discarded and replaced by a normal Draw. In Battle it must immediately be placed into one of the actor's legal, open Strike Squares (maximum three dice per square) and then becomes available in the suffix. **Proposed guardrail:** if the actor has no Battle slot at acquisition time, a to-Hand effect cannot execute (do not silently redirect the acquired die to Discard). This makes the four destinations semantically distinct. A subsequent Draw with no placement space follows the existing drawn-die-to-Discard fallback. Alternative policy (allow acquire-to-Discard fallback) requires an explicit decision before Task 2.
5. Opponent Hand D4 stealing in Battle may open a Strike slot, but does not cause a replacement for the victim. A stolen discard D4 never changes the victim's Strike total. Changing ownership must not duplicate dice or leave a dangling grid reference.
6. The suffix happens **after** acquisition. Selecting zero Hand D4s is legal. Multiple selected D4s are exact individual dice; each is discarded once. A purchased/acquired/Drawn die should be represented by exactly one canonical die instance in its current zone.

## O Edelweiss — two variants

Shared: `Twice, you may Play another grafted Plant card or Flip another card.` Each of up to two choices is determined sequentially; legal choices are recomputed after the first resolves. `Done` is always offered and terminates immediately. `Flip` toggles any eligible other grafted Plant, whether face-up or face-down; it does not run that card's effect. `Play` resolves another Plant's current configured effect without spending an additional Main Action. A Play target must have an executable effect in the current phase/context. The target cannot be the source O Edelweiss itself or another Plant on the active nested-effect execution path; cycle protection must hold through recursively triggered effects such as another O Edelweiss/Vine and Again. Stale and illegal choices are rejected.

| ID | Proposed stable GameEffect constant | Play eligibility | Resulting facing |
| --- | --- | --- | --- |
| EDEL-O1 | `PLAY_SPENT_OR_FLIP_ANOTHER_CARD_TWICE` | Another currently **face-down** grafted Plant only | Remains face-down |
| EDEL-O2 | `PLAY_ANY_OR_FLIP_ANOTHER_CARD_TWICE` | Another **face-up or face-down** grafted Plant | **Proposed:** retain prior facing; face-up remains up, face-down remains down |

`EDEL-O1` is existing `PLAY_OR_FLIP_ANOTHER_CARD_TWICE`. In O1, Flip a face-up card down on choice 1 and then Play it on choice 2 is legal; two Plays of the same face-down card may be legal if its effect remains executable. In O2 a face-up card may be Played once or twice without being turned face-down *by the O Edelweiss Play mechanism*. A nested effect that independently changes card facings must still be honored, except active-path cycle safeguards. **Design decision to ratify before Task 3:** whether O2 face-up Play should instead spend (turn down) the card. This specification uses *preserve facing* for fair comparison against O1, consistent with the previously outlined proposal, but the source does not yet implement or establish that rule.

**Executable guard:** the old handler `canExecute` requires at least one eligible other grafted card to Flip. For O2 continue to require at least one eligible non-active target (Play or Flip), rather than allowing empty effect activations simply because Done is allowed. During choice 2, if no Play or Flip remains, Done still ends cleanly. If a nested effect removes or alters targets, rebuild eligibility.

## Sapping Snapdragon — two existing variants

Both select one of the actor's current Hand dice and raise it by +2, capped at the die's maximum. During Cultivation, there is **no Strike component**: only the +2 Raise. During Battle, both modify opposing dice in one row by -2 each, not below 1; accumulate the **actual** reductions, then Raise a selected actor die by that total, capped at maximum. Reductions are applied to every opponent die currently in that row, including multiple dice in a Strike Square; unaffected rows remain unchanged; empty opposing squares contribute 0. A Raise/Reduce/Set does not roll a die and generates no Roll Reward. Neither version moves any die.

| ID | Existing GameEffect constant | Battle anchor row | Recipient of accumulated Raise | Minimum actor dice to execute in Battle |
| --- | --- | --- | --- | --- |
| SNAP-S1 | `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW` | Row of the **same initially raised die** | That **same die** | 1 |
| SNAP-S2 | `RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW` | Row of a **different second actor die** | That **second die** | 2 |

SNAP-S1 is the current `resync-current.csv` choice for `Flower_11_03`. SNAP-S2 is implemented but not selected by that baseline. In S2, first select and Raise die A +2, then select *different* die B; B determines the row and receives any accumulated Raise. B can occupy the same row as A, but must be a different physical die. If only one actor Battle die exists, S2 is not executable. Neither variant requires an opponent die to exist in the target row: reduction total may be zero, while the original +2 still happens. All Battle candidates must belong to the actor and be actually placed on the Battle Grid. Closed/resolved Strike Row handling should follow central Battle candidate/legality rules; this source code does not explicitly establish an independent closed-row exclusion inside these handlers, so verify via regression tests rather than inventing an extra rule.

## Baseline and research non-goals for Task 1

- Do **not** edit `GameEffect.kt`, converter/dispatcher/handler, research resync CSV, card names, VP scoring, data files or frozen policy weights in this task.
- A future Task 2 must specify actual `GameEffect` registration, source-choice exposure, Hand/Discard transfer, Battle placement, scoring invariance, and tests. Task 3 implements both O variants; Task 4 verifies both Sapping variants.
- Human-baseline *choices* are **not** defined by this rules specification. The future baseline audit must independently verify that the existing Human decision strategy can consider each newly legal branch and target without introducing a gratuitous preference.
- Log selected variant identifier along with effect code in future experiment manifests; record whether a variation changes actual decisions and game outcomes, not only aggregate VP.

## Open decisions, called out explicitly

1. **P2/P4 Battle with all Strike Squares full:** proposed effect ineligible rather than auto-redirect acquired D4 to Discard. Confirm before code changes.
2. **O2 replay of face-up Plant:** proposed keep facing up (effect does not spend it). Confirm before implementation; it could be particularly powerful.
3. **Battle redraw when all squares full:** current redesigned Petal sends each newly drawn die to Discard; retain that legacy behavior for all versions unless separately changed.

All other entries above are either observed current semantics or explicitly proposed consistent extensions, not claims that unimplemented versions already work.
