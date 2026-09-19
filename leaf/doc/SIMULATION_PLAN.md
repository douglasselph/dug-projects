# Simulation Plan

## 1. Purpose

The Leaf & Let Die simulator is a **game-design experiment harness**. Its purpose is to find evidence of balance and strategy problems that ordinary playtesting may miss or may require too many human games to measure reliably.

It is not expected to mathematically prove that no exploit exists. Instead, it should identify red flags such as:

- dominant strategies;
- forced-buy cards;
- trap cards;
- runaway-leader effects;
- turn/seat/Battle-order bias;
- strategies that are too simple for how well they perform;
- luck overwhelming meaningful player choice;
- resources or effects whose early acquisition creates disproportionate win advantage.

A useful working definition is:

> **An exploit is a simple, repeatable strategy or game condition that produces an unusually large advantage across many games and remains strong across varied setups or reasonable counterplay.**

The deepest project question is not whether AI can “solve” Leaf & Let Die. It is whether simple or accidental patterns can beat the interesting decisions the game is supposed to reward.

---

## 2. Master research question: skill versus luck

A central measurement is whether stronger decision making produces a clearer signal than ordinary random variation.

Conceptually:

```text
skill signal = performance change caused by better decisions
luck signal  = performance change caused by dice / early draws / setup / order
```

The simulator should support paired experiments such as:

- same strategies, different mechanical seeds;
- same mechanical seed, different strategies;
- same seed/state, different seat assignment;
- same Grove, different Round Deck;
- same Round Deck, different Grove;
- same Battle state, swapped columns/order.

The goal is not to eliminate luck. Leaf & Let Die is intended to have uncertainty, swinginess, and drama. The design concern is whether meaningful decisions remain visible through that randomness.

---

## 3. Research program

### 3.1 Dominant purchase paths

Question:

> Can a simple “buy the biggest die possible” or similarly narrow economic policy outperform the interesting Plant-card system?

Candidate comparisons:

- Big-Dice policy vs Human Baseline;
- Big-Dice policy vs card-focused policy;
- Big-Dice policy vs tactical Battle policy;
- Big-Dice policy vs random-legal play;
- mirror matches.

Repeat across different Groves and Round structures.

### 3.2 Forced-buy cards

For each Plant card, eventually measure quantities such as:

- availability win delta;
- forced-priority win delta;
- first-buy frequency;
- purchase frequency;
- activation/use rate;
- copies owned at end;
- interaction with other cards/strategies.

Controlled variants should include:

1. normal availability;
2. card removed;
3. one player encouraged/forced to prioritize it;
4. all players prohibited from buying it.

The existing `CardFocusExperiment` is the first concrete foundation for this research family.

### 3.3 Trap cards

Look for cards that appear attractive but are systematically poor choices.

Useful signals include:

- strong/baseline strategies rarely buy the card;
- card acquisition correlates with worse outcomes in controlled comparisons;
- the card is bought but rarely activated;
- opportunity cost consistently exceeds its benefit.

Avoid inferring causation merely from “players who bought this card lost more.” Use paired or forced-priority comparisons where practical.

### 3.4 Strategy diversity

The game should permit multiple viable strategic families rather than collapsing into one correct script.

Candidate families include:

- dice-development emphasis;
- Plant-engine emphasis;
- Battle/Strike VP emphasis;
- Wound pressure;
- Wisp preservation/use;
- support-resource hoarding;
- aggressive resource spending;
- balanced Human Baseline;
- later tactical/strategic variants.

Important questions:

- Which strategies have favorable and unfavorable matchups?
- Does one low-complexity family beat nearly everything?
- Do multiple families remain viable across random Groves and seats?
- Does adaptation outperform rigid scripting?

### 3.5 Luck resistance

Measure how sensitive game outcomes are to:

- dice rolls;
- Wisp/Critter Roll Rewards;
- Grove composition;
- Round Deck order;
- seat/Battle order;
- strategic decisions.

Paired seeds and the split mechanical/strategy RNG architecture are especially important here.

### 3.6 Runaway leader and comeback behavior

At each meaningful checkpoint, record current standing and eventual result.

Potential metrics:

- probability that current leader eventually wins;
- probability that last place eventually wins;
- comeback rate after each Battle;
- score-gap distribution by round;
- remaining meaningful decision opportunities after falling behind.

A leader becoming increasingly likely to win is normal. The warning sign is when the result is effectively settled long before the game ends.

### 3.7 Wounds

Wounds both damage a Plant Creature and can award bonus VP to the winner of a Strike. Measure how much of the game they carry.

Potential metrics:

- win share after suffering the first Wound;
- win share after causing the first Wound;
- average Wounds caused/taken;
- VP attributable to Wound bonuses;
- frequency with which a Wound changes the eventual winner;
- comeback rates after being Wounded.

### 3.8 Battle order / column bias

Battle order can be confounded with strong opening dice because the higher-ranked player is also placed earlier/leftward.

Use mirrored state experiments:

```text
same Battle state
same dice
same resources
same strategies
swap positions/order
replay
```

Record:

- column/order position;
- opening dice strength;
- Main/Support action order;
- Strike VP;
- Wounds;
- support usage;
- final game outcome.

The goal is to separate advantage caused by better dice from advantage caused by acting at a particular time.

### 3.9 Adaptive play versus autopilot

Compare rigid scripts with policies that respond to the actual state.

Example rigid behavior:

- always buy the most expensive item;
- always prioritize upgrades;
- always spend a Bee when it improves a row;
- always use support immediately.

Adaptive behavior can consider:

- whether a Strike is already secure;
- whether a row is too expensive to contest;
- current Creature/dice development;
- remaining Battle timing;
- resource reserves.

A concerning result would be a very simple script performing almost as well as a strategy that actually evaluates the board.

---

## 4. New priority experiment: early-Wisp windfall

One specific concern is whether an unusually large number of early rolls of **2**, and therefore early Wisp gains, can create a near-automatic victory.

This should be treated as a **stress test**, not as a claim about the normal frequency of the event.

### Hypothesis

> If one player receives an extreme but legal early concentration of Wisp rewards, does the resulting advantage remain so large that the rest of the game has little ability to overcome it?

### Initial intervention

Construct a paired simulation in which:

- all players use the same ordinary baseline strategy family;
- the game uses the same Grove/Round configuration;
- one designated player receives artificially Wisp-heavy die outcomes during the **first two rounds**;
- everything after that initial window returns to the normal mechanical random stream;
- the control game uses the unmodified mechanical outcomes;
- the favored seat is rotated across players.

The exact intervention should be parameterized rather than hard-coded. For example:

```text
WispWindfallSpec
  affectedRounds = 2
  affectedPlayer = rotating seat
  forcedTwos = N             // or a precise scripted reward pattern
  gamesPerSeat = ...
  baseSeed = ...
```

Before choosing `N`, add an integration test proving that the intervention produces exactly the intended Roll Rewards without perturbing unrelated mechanics.

### Metrics

At minimum collect:

- affected player's winner rate and win share;
- VP delta versus matched control;
- Wisps gained during the intervention;
- total Wisps gained during the game;
- Wisps played;
- Wisps Trashed;
- Wisps retained for final scoring;
- final VP from Wisps;
- Butterfly gains attributable to Wisps;
- downstream Battle VP and Wounds;
- Wisp Reckoning frequency and effect;
- result by seat;
- result by size of the forced-Wisp advantage.

### Dose-response series

Do not test only one extreme condition. Run a gradient, for example:

```text
control
+1 extra early Wisp
+2 extra early Wisps
+3 extra early Wisps
...
extreme early-Wisp case
```

The shape of the curve is more informative than a single point. A small, roughly proportional advantage is different from a threshold where the win rate suddenly jumps.

### Why this matters to the Wisp-limit question

The experiment can directly inform whether a hard Wisp-hand limit is needed. The relevant finding is not simply “more Wisps are good”; they should be. The concern is whether early Wisp accumulation creates a nonlinear, self-reinforcing or end-game-scoring advantage that becomes difficult for normal play to answer.

Also compare the result with the natural pressure already created by **Wisp Reckoning**. If that card sufficiently checks hoarding in realistic games, a hard hand cap may be unnecessary; if extreme Wisp accumulation remains overwhelmingly strong despite the existing check, that is stronger evidence for changing the rules.

---

## 5. Reporting goals

As experiment coverage grows, the simulator should be able to produce recurring reports.

### Balance report

- win share by strategy;
- average final VP;
- VP by source;
- Wounds caused/taken;
- Plant cards bought;
- dice gained/upgraded;
- Wisps gained/played/retained.

### Strategy report

- matchup matrix;
- favorable/unfavorable matchups;
- strategy performance across Groves;
- performance by player count;
- performance by seat;
- strategy-complexity versus performance.

### Card report

- availability delta;
- acquisition/purchase rate;
- first-buy frequency;
- activations;
- final copies;
- VP/effect contribution;
- cards consistently avoided or consistently prioritized.

### Luck-resistance report

- paired-strategy results with fixed mechanical seeds;
- mechanical-seed sensitivity;
- seat/order sensitivity;
- Grove/Round sensitivity;
- comeback rate;
- winner predictability by round.

### Exploit report

- low-complexity strategies with unusually high performance;
- forced-buy behavior;
- abnormal card/combo results;
- interventions with large persistent win-share deltas;
- strategies that remain strong after obvious counters are introduced.

---

## 6. Development stages

The project should continue to grow in stages rather than jumping directly to sophisticated AI.

### Stage A — correct and observable engine

- legal action generation;
- exact state transitions;
- typed Chronicle;
- deterministic scenario control;
- snapshots/assertions.

Much of this foundation now exists.

### Stage B — meaningful Human Baseline

Create a simple but realistic control strategy. Its purpose is not optimality; its purpose is to avoid obviously irrational behavior that would make simulation conclusions misleading.

The current baseline architecture already contains shared scoring primitives, shared heuristics, card-specific priorities, and strategy-only tie randomness.

### Stage C — focused experiments

Build narrow, interpretable experiments first:

- focused card acquisition;
- early-Wisp windfall;
- big-dice purchase pressure;
- Battle-order mirroring;
- Wound leverage;
- resource-hoarding versus spending.

### Stage D — general batch/tournament reporting

Expand the generic `BatchRunner`/`Matchup`/`Tournament` boundary into practical high-volume runners and common reports.

### Stage E — tactical search

Search is most promising first in constrained Battle windows where legal actions and outcomes are tractable and the result is easy to interpret.

### Stage F — learned/adaptive strategies

Only after the simpler research framework is producing useful evidence should learning systems become a major focus. Interpretability remains more valuable for game design than raw playing strength.

---

## 7. Near-term experiment backlog

A practical sequence is:

1. Establish the Human Baseline as the stable control strategy.
2. Finish a reusable general paired-experiment/batch runner.
3. Run card-focused experiments on representative cards.
4. Add the **early-Wisp windfall** experiment.
5. Compare big-dice emphasis versus baseline/card development.
6. Add mirrored Battle-order experiments.
7. Add Wound leverage and comeback tracking.
8. Add recurring strategy matchup matrices.
9. Add Grove/round-structure variation.
10. Only then decide which questions justify tactical search or learning.

The core principle throughout is: **change one thing, control what you can, rotate confounding positions, run enough games, and record why the outcome changed.**
