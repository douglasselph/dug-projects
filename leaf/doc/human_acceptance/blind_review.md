# C04 — Revised realistic Battle decision scenarios

**Version 2; replaces the earlier 28 scenario specifications.** Every player has at least three explicitly placed dice. No row starts empty. TOP/MIDDLE/BOTTOM show dice, not ambiguous totals.

**Decision stages:** `FIRST` = choose first ordinary Main. `FINAL` = choose second ordinary Main. `SUPPORT WINDOW` = after first Main, choose Support or proceed to Final Main. `TARGET DURING FIRST MAIN` = choose a legal target while resolving an already-selected first Main action.

**Status:** Human-review specifications only. They have NOT been checked against live legal-action generation, supply state, card catalog, strike-square capacity, or Kotlin tests. Both-option Round comparisons may be counterfactual alternatives from two possible Round cards, not a single actionable in-game request.

**Important:** Player A is always the decision maker. Dice listed are the three starting placed dice per player; more may have been acquired later, but none are presumed unless specified. Plant and resource holdings appear in scenario context. Do not use these as frozen training/held-out acceptance fixtures until their legality is confirmed.

**BLIND REVIEW: proposed preferences deliberately omitted.**

## C04-H01 — Development
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** SUPPORT WINDOW

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 5 | D6 showing 4 |
| B | D6 showing 3 | D8 showing 6 | D6 showing 5 |

**Relevant game-state facts:** A has 1 Worm in hand; TOP has room for a Worm; available D4 in Dice Supply; only this Worm Support or Final Draw offered.

- **Option A:** Place a Worm on TOP now
- **Option B:** Proceed to Final Main: Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H02 — Development
**Decision maker:** Player A  |  **Players:** 3  |  **Decision point:** SUPPORT WINDOW

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 7 | D8 showing 5 | D6 showing 4 |
| B | D6 showing 2 | D6 showing 2 | D6 showing 2 |
| C | D6 showing 3 | D6 showing 4 | D6 showing 3 |

**Relevant game-state facts:** A has 1 Worm; TOP already leads substantially; TOP accepts Worm; D4 available in Dice Supply; Final Main closes Support.

- **Option A:** Place a Worm on TOP now
- **Option B:** Proceed to Final Main: Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H03 — Development
**Decision maker:** Player A  |  **Players:** 4  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 4 | D8 showing 5 | D6 showing 2 |
| B | D12 showing 9 | D8 showing 5 | D6 showing 2 |
| C | D8 showing 6 | D8 showing 4 | D6 showing 4 |
| D | D6 showing 3 | D8 showing 5 | D6 showing 3 |

**Relevant game-state facts:** Root Four More face up; D6 drawable; all affected rows accept current pieces; First Main not used.

- **Option A:** Activate Root Four More on own TOP D8 showing 4
- **Option B:** Draw a D6

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H04 — Held Out
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 6 | D8 showing 4 | D8 showing 6 |
| B | D12 showing 9 | D8 showing 5 | D8 showing 6 |

**Relevant game-state facts:** Plant face up; target legal but capped; D8 drawable.

- **Option A:** Activate Root Four More on own TOP D6 showing 6
- **Option B:** Draw a D8

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H05 — Development
**Decision maker:** Player A  |  **Players:** 3  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 2 | D8 showing 4 | D8 showing 6 |
| B | D8 showing 5 | D8 showing 5 | D8 showing 6 |
| C | D6 showing 4 | D6 showing 3 | D8 showing 4 |

**Relevant game-state facts:** Plant face up; D4 drawable; TOP is 2 vs opponents 5 and 4.

- **Option A:** Activate Root Four More on own TOP D6 showing 2
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H06 — Development
**Decision maker:** Player A  |  **Players:** 4  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D6 showing 4 |
| B | D8 showing 6 | D8 showing 7 | D6 showing 5 |
| C | D6 showing 5 | D8 showing 5 | D8 showing 6 |
| D | D8 showing 6 | D8 showing 5 | D6 showing 3 |

**Relevant game-state facts:** Exactly 1 Wisp in shared supply, A below 5-Wisp cap; D4 drawable; First Main unused.

- **Option A:** Use Round effect: gain 1 Wisp
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H07 — Held Out
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D6 showing 4 |
| B | D8 showing 6 | D8 showing 7 | D6 showing 5 |

**Relevant game-state facts:** Shared Wisp supply empty; assume taking zero-effect Round action remains legal, otherwise remove A from choices; D4 drawable.

- **Option A:** Use Round effect: gain 1 Wisp
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H08 — Development
**Decision maker:** Player A  |  **Players:** 3  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D8 showing 6 |
| B | D8 showing 5 | D6 showing 4 | D8 showing 8 |
| C | D6 showing 4 | D8 showing 5 | D6 showing 5 |

**Relevant game-state facts:** B holds 2 Wisps; A below cap; D4 drawable; First Main unused.

- **Option A:** Use Round effect: steal 1 Wisp from Player B
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H09 — Development
**Decision maker:** Player A  |  **Players:** 4  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D8 showing 6 |
| B | D8 showing 5 | D6 showing 4 | D8 showing 8 |
| C | D6 showing 4 | D8 showing 5 | D6 showing 5 |
| D | D8 showing 6 | D8 showing 6 | D8 showing 7 |

**Relevant game-state facts:** None of B/C/D holds any Wisps; A has room; D4 drawable. If stealing requires a target, treat A as illegal and exclude it.

- **Option A:** Use Round effect: steal 1 Wisp from Player B
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H10 — Held Out
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D8 showing 6 |
| B | D8 showing 5 | D6 showing 4 | D8 showing 8 |

**Relevant game-state facts:** Two future Battles remain after this one; D20 in Grove; A has reasonable future opportunity to cycle Discard. This is a counterfactual two-effect comparison, not a claim both are simultaneously printed on one Round card.

- **Option A:** Use Round effect: gain a D20 to Dice Discard
- **Option B:** Use Round effect: gain +1 immediate VP

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H11 — Development
**Decision maker:** Player A  |  **Players:** 3  |  **Decision point:** FINAL

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D8 showing 6 |
| B | D8 showing 5 | D6 showing 4 | D8 showing 8 |
| C | D6 showing 4 | D8 showing 5 | D6 showing 5 |

**Relevant game-state facts:** No future Battle or other action to draw gained die; D20 in Grove. Abstract counterfactual across two possible Round effects.

- **Option A:** Use Round effect: gain D20 to Dice Discard
- **Option B:** Use Round effect: gain +1 immediate VP

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H12 — Development
**Decision maker:** Player A  |  **Players:** 4  |  **Decision point:** FINAL

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D12 showing 10 | D12 showing 9 | D12 showing 8 |
| B | D6 showing 2 | D6 showing 2 | D6 showing 3 |
| C | D6 showing 3 | D6 showing 4 | D6 showing 2 |
| D | D6 showing 4 | D6 showing 3 | D6 showing 4 |

**Relevant game-state facts:** D4 drawable but all own Strike placements assumed legally full for this fixture; no other late Draw benefits. Confirm draw legality with coordinator before execution.

- **Option A:** Use Round effect: gain +1 immediate VP
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H13 — Held Out
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 7 | D8 showing 6 | D8 showing 7 |
| B | D6 showing 5 | D8 showing 6 | D6 showing 5 |

**Relevant game-state facts:** D20 drawable; all own Strike placement capacity full; no other actionable uses of drawn die. This is an exceptional late-stage state, not an initial Battle setup.

- **Option A:** Draw a D20
- **Option B:** Use Round effect: gain +1 immediate VP

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H14 — Development
**Decision maker:** Player A  |  **Players:** 3  |  **Decision point:** SUPPORT WINDOW

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 6 | D8 showing 4 | D8 showing 5 |
| B | D8 showing 6 | D8 showing 7 | D6 showing 6 |
| C | D6 showing 5 | D6 showing 5 | D8 showing 7 |

**Relevant game-state facts:** A holds 1 Sunlight; 2 future Battles; D4 drawable; FIRST already resolved; Sunlight Support is legal; all rows have normal available space.

- **Option A:** Spend the final Sunlight to fund an extra Main: Draw D4
- **Option B:** Preserve Sunlight; proceed to ordinary Final Main

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H15 — Development
**Decision maker:** Player A  |  **Players:** 4  |  **Decision point:** SUPPORT WINDOW

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 6 | D8 showing 4 | D8 showing 5 |
| B | D8 showing 6 | D8 showing 7 | D6 showing 6 |
| C | D6 showing 5 | D6 showing 5 | D8 showing 7 |
| D | D8 showing 7 | D8 showing 6 | D6 showing 5 |

**Relevant game-state facts:** A holds 1 Sunlight; this is final Battle; D20 drawable; FIRST resolved; placement opportunity in contested TOP.

- **Option A:** Spend Sunlight to fund an extra Main: Draw D20
- **Option B:** Preserve Sunlight; proceed to ordinary Final Main

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H16 — Held Out
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** SUPPORT WINDOW

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 6 | D8 showing 4 | D8 showing 5 |
| B | D8 showing 6 | D8 showing 7 | D6 showing 6 |

**Relevant game-state facts:** A holds 3 Sunlight; D8 drawable; 2 future Battles; FIRST resolved; Sunlight Support legal.

- **Option A:** Spend 1 of 3 Sunlight to fund an extra Main: Draw D8
- **Option B:** Preserve all Sunlight; proceed to ordinary Final Main

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H17 — Development
**Decision maker:** Player A  |  **Players:** 3  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 5 | D6 showing 4 |
| B | D6 showing 3 | D8 showing 6 | D6 showing 5 |
| C | D6 showing 4 | D8 showing 4 | D6 showing 3 |

**Relevant game-state facts:** Shared Grove Worm supply empty; A holds none; D4 drawable; First Main unused.

- **Option A:** Use Round effect: gain 2 Worms
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H18 — Development
**Decision maker:** Player A  |  **Players:** 4  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 5 | D6 showing 4 |
| B | D6 showing 3 | D8 showing 6 | D6 showing 5 |
| C | D6 showing 4 | D8 showing 4 | D6 showing 3 |
| D | D6 showing 5 | D8 showing 6 | D6 showing 3 |

**Relevant game-state facts:** At least 2 Worms in Grove; A holds none; TOP tied 3:3 against B; Support will remain after First Main; D4 drawable; TOP accepts Worm.

- **Option A:** Use Round effect: gain 2 Worms
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H19 — Development
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** FINAL

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 5 | D6 showing 4 |
| B | D6 showing 3 | D8 showing 6 | D6 showing 5 |

**Relevant game-state facts:** Final Battle; no further Support after Final Main and no future Battle; Worms available in supply; two hypothetical Round choices.

- **Option A:** Use Round effect: gain 2 Worms
- **Option B:** Use Round effect: gain +1 immediate VP

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H20 — Held Out
**Decision maker:** Player A  |  **Players:** 3  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D8 showing 5 |
| B | D8 showing 5 | D8 showing 6 | D8 showing 7 |
| C | D6 showing 4 | D8 showing 5 | D6 showing 4 |

**Relevant game-state facts:** All A Plants are already face up; D6 drawable; no spent Plant target. Confirm refresh action legal when it has no effect.

- **Option A:** Activate Round/Plant refresh effect
- **Option B:** Draw a D6

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H21 — Development
**Decision maker:** Player A  |  **Players:** 4  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D8 showing 5 |
| B | D8 showing 5 | D8 showing 6 | D8 showing 7 |
| C | D6 showing 4 | D8 showing 5 | D6 showing 4 |
| D | D8 showing 6 | D8 showing 6 | D8 showing 7 |

**Relevant game-state facts:** Root Four More currently face down, another Plant with refresh face up; own TOP D6 showing 3 with opponents TOP 5 and 4; D4 drawable; the Final Main will be available to activate refreshed Root Four More.

- **Option A:** Activate a Plant whose effect refreshes Root Four More
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H22 — Development
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** FINAL

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D8 showing 5 |
| B | D8 showing 5 | D8 showing 6 | D8 showing 7 |

**Relevant game-state facts:** Final Battle; no future Main or Battle after this action; legal refresh effect assumed but no time to reuse Plant.

- **Option A:** Use refresh effect on spent Plant
- **Option B:** Use Round effect: gain +1 immediate VP

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H23 — Development
**Decision maker:** Player A  |  **Players:** 3  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 7 | D8 showing 5 | D8 showing 6 |
| B | D8 showing 8 | D8 showing 5 | D8 showing 7 |
| C | D6 showing 5 | D8 showing 6 | D8 showing 6 |

**Relevant game-state facts:** A TOP 7, B TOP 8, C TOP 5; reduction permitted against B; D4 drawable; First Main unused.

- **Option A:** Activate Plant to reduce Player B TOP Strike total by 3
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H24 — Held Out
**Decision maker:** Player A  |  **Players:** 4  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 7 | D8 showing 5 | D8 showing 6 |
| B | D8 showing 8 | D8 showing 5 | D8 showing 7 |
| C | D6 showing 5 | D8 showing 6 | D8 showing 6 |
| D | D8 showing 7 | D6 showing 6 | D8 showing 7 |

**Relevant game-state facts:** No opponent die meets flip eligibility, although the effect is available; D4 drawable; First Main unused. Confirm whether Plant may legally activate with zero targets.

- **Option A:** Activate opponent-flip Plant
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H25 — Development
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D12 showing 4 | D8 showing 3 | D8 showing 6 |
| B | D12 showing 6 | D12 showing 8 | D8 showing 6 |

**Relevant game-state facts:** Gust face up; opposing TOP D12 showing 6 is eligible for reroll; D4 drawable; First Main unused.

- **Option A:** Activate Gust of Petals targeting own TOP D12 showing 4
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H26 — Development
**Decision maker:** Player A  |  **Players:** 3  |  **Decision point:** FIRST

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 3 | D8 showing 3 | D8 showing 6 |
| B | D12 showing 9 | D12 showing 8 | D8 showing 6 |
| C | D8 showing 5 | D8 showing 7 | D6 showing 6 |

**Relevant game-state facts:** Bloom face up; opponent B TOP D12 showing 9 eligible; D4 drawable; First Main unused.

- **Option A:** Activate Bloom Backflip targeting own TOP D8 showing 3
- **Option B:** Draw a D4

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H27 — Held Out
**Decision maker:** Player A  |  **Players:** 4  |  **Decision point:** TARGET DURING FIRST MAIN

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D8 showing 3 | D8 showing 2 | D8 showing 4 |
| B | D8 showing 3 | D8 showing 8 | D8 showing 7 |
| C | D6 showing 3 | D8 showing 6 | D8 showing 6 |
| D | D6 showing 4 | D8 showing 5 | D6 showing 5 |

**Relevant game-state facts:** A owns relevant Plant; B has TOP 3 and MIDDLE 8; both candidate row targets legal. These are TARGET choices after the same Main activation, not separate Main actions.

- **Option A:** Activate set-to-3/reduce-row Plant, choosing TOP for opponent reduction
- **Option B:** Activate same Plant, choosing MIDDLE for opponent reduction

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---

## C04-H28 — Development
**Decision maker:** Player A  |  **Players:** 2  |  **Decision point:** FINAL

**Already placed dice (one per row per player):**

| Player | TOP | MIDDLE | BOTTOM |
|---|---|---|---|
| A | D6 showing 3 | D8 showing 4 | D8 showing 5 |
| B | D6 showing 3 | D8 showing 5 | D8 showing 5 |

**Relevant game-state facts:** Final Battle, ordinary Final Main; Support ends after this action; Worms in Grove; no future use; two alternative Round-effect counterfactuals.

- **Option A:** Use Round effect: gain 2 Worms
- **Option B:** Use Round effect: gain +1 immediate VP

**Question:** Which action should Player A prefer **at the stated decision point**, and why?

**Your preference:** A / B / Tie / Needs more information

**Confidence:** ______  **Reasoning / corrections:** __________________________

---
