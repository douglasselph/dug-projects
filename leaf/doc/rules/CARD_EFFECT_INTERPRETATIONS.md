# Card Effect Interpretation Register

**Prepared:** November 6, 2026  
**Status:** Implementation recommendations; **design choices marked PROPOSED require designer approval before being treated as adopted game rules**.  
**Scope:** Revised official Plant, Wisp, and Round catalogs in `data/` and the adopted v38 Three Engagements Battle rules. This document is a durable rules-interpretation reference, not a historical task log.

## Authority and change control

The four official catalogs (`Cultivation_Cards - Root.csv`, `Cultivation_Cards - VF.csv`, `Wisp_Cards.csv`, `Round_Cards.csv`) are authoritative **for current card text and identity**. The v38-01 rulebook remains authoritative for *general game procedure*, unless an updated card expressly changes it. Its Almanac descriptions for revised cards are historical and must not overwrite the CSVs. Research/resync overrides are not official rules.

Relevant rulebook pages: **pp. 8–11** (Roll Rewards, helpers and upgrades), **pp. 15–17** (Battle Hand, Strike capacity, once-per-Engagement turn, actions, cleanup), **pp. 19–21** (older individual-card/Wisp examples), and **pp. 22–23** (glossary). Relevant current code includes `ThreeEngagementBattle`, `UpgradeResolver`, `SupportActionExecutor`, `DrawEffectHandler` and `WispReckoningEffect`; existing implementation is *evidence of behavior*, not authority for new semantics.

**This register does not authorize implementation of a proposed ruling as canonical.** Resolve any PROPOSED item with the designer, then mark it ACCEPTED (with the date); update affected tests and any card wording if needed. Implement existing settled rules without waiting for unrelated choices.

## Established procedural rules (SETTLED)

| Key | Rule already established by the adopted Battle structure | Implementation consequence |
| --- | --- | --- |
| BASE-TURN | Each player has **one turn per Strike**, with up to **one normal Main** and **two paid Supports** in any order, plus any separately granted extra Main. Only **two normal Main tokens** are available per entire Battle. | Free Support must happen *inside* the owner's one turn, not by reopening an Engagement after passing. Sunlight still consumes a paid Support for its extra Main. |
| BASE-ROWS | Target only the current or later unresolved Strike; a resolved Strike is locked, Doomed and cleared. | An effect cannot add/remove/reroll/flip a die in a finished row. Row closure during a nested action ends later opportunities targeting that row. |
| BASE-CAP | Each player's Strike Square holds at most **three dice** (any number of Critters). Newly added Battle Hand dice need a legal open Strike Square. | There is no implied 'fourth die' slot or automatic overflow location. |
| BASE-ROLL | Roll Rewards apply whenever a die is *rolled*, including rolls caused by effects, except when expressly suppressed. Merely changing a face via Raise/Set/Flip is not a roll. | An immediate-use die that is rolled earns a Critter on 1 or Wisp on 2. |
| BASE-GRAFT | 'Grafted' counts currently attached Plant cards, face-up **and** face-down; Snipped cards are gone. | Count the currently grafted card that generated the effect, unless it was removed before resolution. |
| BASE-SUPPLY | A physical component can only be gained from an available source; its normal use still spends/flips/removes it. | 'Free Support' waives an action cost, **not** an inventory/ready-state cost. |

## Recommended clarifications requiring designer approval

### FREE — Bee, Worm, and Butterfly Battle Support discounts

**Cards:** Root Appreciation (`Root_07_02`), Bee-loved Bloom (`zFlower_14_01`), Alluring Nectar (`zFlower_11_01`).

- **FREE-1 — PROPOSED:** Once one of these Plant effects *resolves*, that player gains the named **zero-Support-cost permission for the remainder of the current Battle Round**, including later Strikes. It begins only after resolution; it is not retroactive. The card subsequently turning face down or being Wounded/Snipped does **not** retroactively cancel this already-resolved round effect. Grafting but never activating the card confers no such permission. The permission expires at Battle end. In Cultivation, these Battle-only clauses have no effect.
- **FREE-2 — PROPOSED:** A free Bee/Worm/Butterfly use costs **zero of the two paid Support slots**. It is legal after two paid Supports have been used, but only during that player's current single Engagement turn and only while the Strike remains open. It does not grant another Main Action, a second turn, or the ability to act after passing. Sunlight continues to cost **one paid Support slot** to grant its extra Main.
- **FREE-3 — PROPOSED:** 'Playing a Worm' covers **both normal Worm uses** during Battle: placing it for Strike strength and spending it to Flip a Plant. 'Using a Bee' applies to its normal Battle placement. 'Using a Butterfly' applies to a ready Butterfly reroll. Each use still consumes a Worm/Bee or flips the Butterfly face down normally; a Butterfly already face down cannot be reused without an actual Refresh.
- **FREE-4 — PROPOSED:** Repeated activation or multiple copies **do not make an already-free action more free**. Root Appreciation's **+1 Worm value per activation this round** and Bee-loved Bloom's **+2 Bee value per activation this round** *do* stack as their separate printed effects specify. A zero-cost permission is a boolean for that player and resource type, not an accumulation of negative action costs.
- **FREE-5 — IMPLEMENTATION GUARD:** Replace the current hard `actionCount <= 3` with explicit accounting for normal Main, paid Support and zero-cost actions. Still require finite component consumption/Butterfly state transitions and stop after Pass, row resolution, or no useful legal action. Trace **paid** versus **free** actions separately. A potential chain that gains a new resource is re-evaluated using current state; no arbitrary unlimited-repeat loop is permissible.

### UPGRADE — Conditional Root Awakening

**Card:** Root Awakening (`Root_07_01`): "Upgrade 1 die by 2 steps if it shows 3; otherwise, 1 step. Use the new die now."

- **UPGRADE-1 — PROPOSED:** 'Steps' are **positions in the ordinary fixed ladder** `D4 → D6 → D8 → D10 → D12 → D20`, not the number of sizes currently stocked. A die showing 3 must upgrade exactly two ladder positions; another face must upgrade exactly one. This **does not inherit** Overgrowth's explicitly different 'skip missing sizes' rule.
- **UPGRADE-2 — PROPOSED:** The chosen final replacement size must be available. It is **not** necessary for the *intermediate* size to be in stock: one source die is exchanged directly for one final die. If the required final size is unavailable, or the required step(s) go beyond D20, that source die is **not a legal target**. Do **not** silently fall back from two steps to one, skip to a larger available size, or trash the source die. If no legal target exists, **proposed:** the Plant activation is not offered as a legal action (so no Main token is spent and the card is not flipped); no partial mutation. This last legality choice is itself designer-dependent, because the older Almanac says an unavailable Upgrade "does nothing."
- **UPGRADE-3 — SETTLED CONSEQUENCE:** Return replaced D4 to the Grove, remove replaced larger dice from the game; the new die replaces it in Hand, is rolled once with Roll Rewards, and in Battle occupies the **same legal open Strike Square** as the old die. Do not count this as adding an extra die to the Square.

### RAISE — Repeated Raise packages

**Cards:** Saplink Trellis (`Vine_09_03`), Bloom Backbone (`zFlower_11_02`), Vine Yield (`Vine_07_04`).

- **RAISE-1 — PROPOSED:** For Saplink Trellis's "For each Root or Vine, Raise 2 dice +1", perform **one iteration per currently grafted Root/Vine**. Within a single iteration, select **two distinct owned dice** if two can benefit; a die may be chosen again in *later* iterations, with the cap reapplied after each +1. **If only one can benefit, Raise that one once; if none, that iteration has no effect**. This interprets 'Raise 2' as 'up to two when fewer can benefit' rather than failing an entire effect. This fallback is **not settled by the printed wording** and needs approval.
- **RAISE-2 — PROPOSED:** Saplink target selection can change each iteration as actual die values change. Each +1 is capped at that die's maximum; no Roll Rewards. In Battle, only the player's dice still in open rows can be targeted. A zero-benefit Raise on an already-maximum die is not counted as a mechanical gain.
- **RAISE-3 — SETTLED/WORDING:** Bloom Backbone counts currently grafted Vines/Flowers (including itself), and applies +1 to **all currently owned eligible Hand dice for each counted card**, capping each die; not one freely chosen die per graft. Vine Yield instead **chooses one die** and Raises that same die by +1 per grafted Flower; it does not distribute those increments among several dice. The existing v38 Almanac's older wording for Bloom Backbone is superseded by the new CSV.

### WISP — Targeting and the card being played

**Cards:** Wisp of Honor (`Wisp_Award_VP`), Berry Patient (`Wisp_Award_VP2`).

- **WISP-1 — PROPOSED:** Wisp of Honor's "Choose 1 player with Wisps" **includes the actor**: the card says *player*, not *opponent*. The chosen player, not the actor, chooses which of their eligible Wisps to Trash. A player with no other eligible Wisp cannot be selected.
- **WISP-2 — PROPOSED:** The Wisp **currently being played** is already committed to being Trashed by the normal play procedure; it is **not an eligible Wisp to Trash again** and must be excluded from all targets' choices. When Wisp of Honor is the actor's sole Wisp, it does not make the actor an eligible target. If *nobody* has any other Wisp, Wisp of Honor has no legal target; treat that as an unavailable play rather than permitting a self-targeting no-op.
- **WISP-3 — PROPOSED:** Berry Patient includes the actor and every opponent who has at least one **other** eligible held Wisp. Each such player independently chooses exactly one to Trash. Resolve choices from a common snapshot (simultaneous intent); apply them deterministically without allowing choices to affect later eligibility. Berry Patient itself is excluded for its actor; if no other player or actor has Wisps, it simply resolves without additional trash and the played card is Trashed normally.
- **WISP-4 — IMPLEMENTATION GUARD:** Current `SupportActionExecutor.playWisp` runs the effect **before** `player.wisps.remove(card)`. Therefore the new effect handler must explicitly exclude the source Wisp (one physical copy, not every copy of the same definition) while making selections. Do not remove the played Wisp early without checking other Wisp mechanics and nested effects. Keep effects atomic when selecting across multiple players.

### NOW — 'Use it now' and Battle capacity

**Cards:** Petal To Die 4 (`zFlower_14_04`), Forget-Me-Not (`zFlower_17_02`), Swell (`Battle_Scatter_Swell`), Root Awakening (`Root_07_01`).

- **NOW-1 — PROPOSED:** For a die gained from the Grove or selected from Discard, 'Use it now' means **move it into the player's current Dice Hand, roll it once, award normal Roll Rewards, and retain the rolled result for current use**. During Cultivation it remains in Hand for Build/Buy; during Battle it must also be placed into an **open Strike Square with room**, chosen legally by the player. It does not jump automatically to Discard. This differs from replacing a die in place (Root Awakening), which needs no additional square capacity.
- **NOW-2 — PROPOSED:** If a die must newly enter the Battle Hand and **no open Strike Square has space at the moment of that step**, it cannot be gained-and-used. Check legality **before irreversible mutations**; don't silently grant the die to Discard, discard another die without an effect instruction, reopen a finished row, or overflow a square. A mandatory 'gain + use now' effect is not offered as an action if it cannot complete its first step. If a nested effect changes the board before placement, re-check current capacity.
- **NOW-3 — PROPOSED:** Petal To Die 4 performs its printed steps **in order**: first gain an available D4 and use it now; *then* optionally discard any number of owned D4s currently in Hand (including the newly gained D4), and draw one die for each discarded D4. In Battle, removal of a discarded D4 from its open Strike Square also frees that square; each subsequently drawn die must be placed into an available open Square and earns its own Roll Reward. If the mandatory initial gain/use cannot happen (e.g. no D4 in Graft Bed or no open Square capacity), do not skip ahead to the discard/redraw step.
- **NOW-4 — PROPOSED:** 'Gain up to a D8/D12' allows choosing any **available** Graft Bed die of size no larger than the limit (including D4 if a D4 is present there). Swell uses that die now, so it requires legal Battle placement; Surge places its chosen die in Discard and does not roll it or need a Battle Square. No available qualifying die means no gain; do not manufacture an unavailable size.

## Proposed acceptance / implementation sequence

1. Obtain designer confirmation or amendments for **FREE-1–4, UPGRADE-1–2, RAISE-1–2, WISP-1–3, NOW-1–4**. Those are recommendations, not decisions implied by the older rules text. Once accepted, change labels to `ACCEPTED YYYY-MM-DD` rather than removing their provenance.
2. Implement core scheduling and action legality together with the three free-action Plant effects; do **not** waive physical token/ready-state costs.
3. Implement upgrade/raise/targeting/immediate-use mechanics using the approved contracts. Maintain the pending-effect fail-closed registry until handlers, decision surfaces and tests actually pass.
4. Add deterministic Kotlin fixture tests from `CARD_EFFECT_SCENARIOS.md`, then run full game and research regressions. These fixtures are **test specifications, not tests already run**.

**Source caveat:** The older v38 PDF does not yet reflect the latest card CSV text (notably Root Awakening, new free-Support clauses, Wisp of Honor/Berry Patient, Petal To Die 4, Swell). This interpretation register cannot silently update that published rulebook.
