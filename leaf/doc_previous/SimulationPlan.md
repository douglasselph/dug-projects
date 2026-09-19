
# SimulationPlan.md

## 🎯 Simulation Objective

The goal of this simulation is to evaluate the **impact of individual cards** in the Cultivation Phase of your game by measuring their effectiveness in a structured and repeatable way. The simulator will help determine:

- Balance between cards and dice.
- Pacing of the game toward Bloom acquisition.
- Relative strength of cards and their effects.
- Impact of new cards introduced to a single player in a competitive simulation.

---

## 📦 Components Required

### 1. **Market Setup**
- Fixed set of cards: one kind per Flourish Type (Seedling, Root, Canopy, Vine, Bloom).
- Each card type will have a "baseline" version:
  - Seedling: Fixed Pip 2 (Resilience 2)
  - Root: Fixed Pip 3 (Resilience 3)
  - Canopy: Fixed Pip 4 (Resilience 4)
  - Vine: Fixed Pip 5
  - Bloom: Fixed Pip 6

### 2. **Player Setup**
- Two players start with the same number of cards and dice.
- Dice start as: 2D4 and 2D6.
- Player A will use the target card(s) under test.
- Player B uses only baseline cards and dice.

---

## 🧠 Simulation Rules

### 🎲 Dice and Roll Rules
- Dice are rolled at **average value** (e.g., D4 = 2.5, D6 = 3.5, etc.)
- The lowest available dice are drawn when drawing from Supply (D4, then D6, etc.).

### 💰 Acquisition Rules
- One action per turn: either acquire a card or a die.
- **Even Rule** determines acquisition:
  - If player has more dice than cards → buy a card.
  - If player has more cards than dice → buy a die.
  - If equal → buy a die.
- Players buy the **most expensive** card/die they can afford.

### 🪴 Card Availability
- Players purchase from a Market of available cards.
- If the desired card is unavailable, move to the next affordable one.
- Bloom acquisition follows standard rules and ends Phase 1 once all Bloom cards are taken.

---

## 🔁 Simulation Strategy

### ✅ Core Single Evaluation
- Run a simulation starting with a fixed hand (e.g., Hand-6).
- Play one turn and calculate the total pips generated.
- Use this as a **baseline comparison** for added card(s).

### 🔄 Core Evolving Evaluation
- Start with Hand-6 (2D4, 2D6).
- Gradually expand to Hand-8, Hand-10, Hand-12, Hand-20.
- For each stage, simulate pip output and compute the average.
- Compare changes over time due to added cards.

### ⚔️ Attacking Evaluation
- Run a head-to-head match:
  - Player A gets the added card(s).
  - Player B only uses baseline deck.
- Measure average pip difference per turn and time to Bloom transition.
- Use the **net advantage** of Player A as a card effectiveness score.

---

## 📊 Evaluation Metrics

- **Total pips per turn** (efficiency metric).
- **Turn count to reach Bloom** (pacing metric).
- **Average hand value over time** (growth metric).
- **Net advantage in competitive simulation** (power metric).

---

## ⚙️ Implementation Suggestions

- Use Kotlin for your simulation logic.
- Create a `Card` class with properties for:
  - Flourish Type
  - Resilience
  - Cost
  - Effect
  - Match Type
  - Matching Effect
- Allow card behaviors to be passed in as lambdas or strategy functions.
- Add logging for:
  - Purchase decisions
  - Hand/dice growth
  - Final pip comparisons

---

## 🧪 Next Steps

- Tag cards by tier or type (baseline, advanced, Bloom).
- Use this simulation to compare:
  - Randomized market vs. fixed scenario markets.
  - Balance impact of powerful effects (e.g., draw dice, steal dice, adjust die to max).
- Refine based on simulation results and adjust cost, match effects, or availability.

---
