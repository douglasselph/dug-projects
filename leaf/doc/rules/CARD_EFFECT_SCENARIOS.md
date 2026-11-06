# Card Effect Mechanical Test Scenarios

**Status:** Proposed Kotlin deterministic fixtures, awaiting approval of the linked interpretation register and subsequent implementation. **No test in this file has been executed.** Use real CSV-backed card definitions; do not create different synthetic text to bypass readiness checks.

See [`CARD_EFFECT_INTERPRETATIONS.md`](CARD_EFFECT_INTERPRETATIONS.md) for authority, proposed decisions, and source mechanics. Each scenario names the intended behavior *if those recommendations are approved*. Keep the stable scenario IDs in test names / failure output.

| Fixture | Setup | Expected result | Contract |
| --- | --- | --- | --- |
| `free-worm-after-two` | Root Appreciation activated; player's turn in High Strike; exactly two paid Supports used; owns 2 Worms | Place a Worm for strength without increasing paid Support count; can spend second Worm to Flip if a target exists; no new turn after pass | FREE-1–3 |
| `free-worm-both-uses` | Root Appreciation activated, one Worm, one face-down Plant | Worm Flip is zero-Support-cost; Worm returns to Grove | FREE-3 |
| `free-multiple-activations` | Root Appreciation activated twice in a Battle | Worm value is normal +2 (two +1 boosts); no negative Support cost; free permission remains one boolean | FREE-4 |
| `free-bee-butterfly-resource` | Bee-loved Bloom and Alluring Nectar activated; 1 Bee, 1 face-up Butterfly; 2 paid Supports already spent | Bee placement and Butterfly reroll free; Bee consumed, Butterfly spent; no second Butterfly reuse without Refresh | FREE-2–4 |
| `free-expires-and-locks` | Free permission active in High; player passes or row finishes early; another Strike remains | Cannot act again in High; permission may be used during their later separate Strike turn; expires after Battle | BASE-TURN, BASE-ROWS, FREE-1 |
| `free-sunlight-still-paid` | Free Worm permission active, player owns Sunlight, 2 paid slots already consumed | Sunlight cannot be spent as a third paid Support, though a Worm remains legal | FREE-2 |
| `upgrade-shows-three` | Root Awakening chooses D4=3; Graft Bed has D8, lacks D6 | Upgrade directly to D8; one new roll and Reward; D4 returned | UPGRADE-1–3 |
| `upgrade-final-unavailable` | Choose D4=3; D6 in stock but D8 unavailable | D4 not a legal Root Awakening target; no one-step fallback | UPGRADE-2 |
| `upgrade-beyond-d20` | Choose D12=3 or D20=3 | Cannot reach two steps higher; no silent one-step downgrade | UPGRADE-2 |
| `upgrade-replace-open-row` | Root Awakening targets an open-row D6=3 in a full three-die Square; D10 available | Replace in same location; Square remains at three dice; Roll Reward if 1/2 | UPGRADE-3 |
| `raise-two-distinct` | Saplink Trellis with 2 qualifying grafts; two eligible dice A,B | Within each iteration A and B may each get +1; A may be used again next iteration; maxima capped | RAISE-1, RAISE-2 |
| `raise-only-one` | Saplink Trellis with multiple qualifying grafts and only one non-max owned die | That die can gain +1 once per iteration until capped; no hard failure for lacking a second candidate | RAISE-1 |
| `raise-grafted-count` | Bloom Backbone active, two grafted Vines and one Flower (including itself), one face-down; three owned dice | Three passes of Raise-all +1; counted face-down Plant included; no completed-row die affected | BASE-GRAFT, RAISE-3 |
| `raise-vine-yield-single` | Vine Yield with three grafted Flowers, two eligible dice | Exactly one chosen die may gain at most +3; not one +1 on each of three dice | RAISE-3 |
| `wisp-honor-target-self` | Player has Wisp of Honor plus another Wisp; all opponents have none | Actor may target self, trash another Wisp; played Honor also Trashed once by normal play | WISP-1–2 |
| `wisp-honor-only-self-card` | Actor has only Wisp of Honor; opponents have no Wisps | Cannot choose played Honor as its own forced victim; no legal target | WISP-2 |
| `wisp-honor-opponent-choice` | Actor plays Honor targeting opponent with two Wisps | Opponent selects the Wisp to Trash (not actor) | WISP-1 |
| `wisp-patient-actor-and-others` | Actor has Berry Patient plus one Wisp; one opponent has two, another none | Actor and holding opponent each select one other held Wisp; no effect on empty opponent; played Patient Trashed only once | WISP-3 |
| `wisp-patient-source-excluded` | Actor's only Wisp is Berry Patient; nobody else has Wisps | Zero additional Wisps trashed; Patient itself Trashed once normally | WISP-2–3 |
| `now-swell-in-open-row` | Swell offered; Graft Bed D8 empty, D6 available; actor has an open Square with one space | Choose D6, gain/roll/Reward/place immediately; no Discard transfer | NOW-1, NOW-4 |
| `now-no-space` | Swell, Forget-Me-Not or Petal gain step; all unresolved actor Squares full | Mandatory gain-and-use unavailable; no automatic Discard fallback, inventory untouched | NOW-2–3 |
| `now-forget-me-not` | Discard has D12, actor open Battle Square with space | Choose D12, remove exact die from Discard, roll/Reward/place into chosen open Square | NOW-1 |
| `now-petal-in-order` | Available D4, open Square; actor already owns D4s and Supply has drawable dice | Gain-roll-place D4, optionally discard that D4 and existing D4s, then draw one per discard and place subject to capacity | NOW-3 |
| `now-locked-row` | One Strike finished, later Square open | No gain/use placement into closed row; another legal open row may be targeted | BASE-ROWS, NOW-1–2 |
| `now-surge-discard` | Surge offered; D12 unavailable, D10 available | Gain chosen D10 directly into Discard, no roll/Reward/placement | NOW-4 |

**Implementation note:** These are *acceptance expectations*, not independent evidence of author intent. If the designer chooses different rulings, update this matrix **before** implementing tests. Include both legal and illegal choices and check state remains unchanged on rejected mandatory effects. Test 2-, 3-, and 4-player Battle as relevant, and source/research provenance of the official CSV definitions.
