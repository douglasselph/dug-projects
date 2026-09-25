# Simulation Plan

## 1. Purpose

The Leaf & Let Die simulator is a **game-design experiment harness**.
Its purpose is to find evidence of balance and strategy problems that
ordinary playtesting may miss or may require too many human games to
measure reliably.

It is not expected to mathematically prove that no exploit exists.
Instead, it should identify red flags such as:

-   dominant strategies;
-   forced-buy cards;
-   trap cards;
-   runaway-leader effects;
-   turn/seat/Battle-order bias;
-   strategies that are too simple for how well they perform;
-   luck overwhelming meaningful player choice;
-   resources or effects whose early acquisition creates
    disproportionate win advantage.

A useful working definition is:

> **An exploit is a simple, repeatable strategy or game condition that
> produces an unusually large advantage across many games and remains
> strong across varied setups or reasonable counterplay.**

The deepest project question is not whether AI can "solve" Leaf & Let
Die. It is whether simple or accidental patterns can beat the
interesting decisions the game is supposed to reward.

------------------------------------------------------------------------

## 2. Master research question: skill versus luck

A central measurement is whether stronger decision making produces a
clearer signal than ordinary random variation.

Conceptually:

``` text
skill signal = performance change caused by better decisions
luck signal  = performance change caused by dice / early draws / setup / order
```

The simulator should support paired experiments such as:

-   same strategies, different mechanical seeds;
-   same mechanical seed, different strategies;
-   same seed/state, different seat assignment;
-   same Grove, different Round Deck;
-   same Round Deck, different Grove;
-   same Battle state, swapped columns/order.

The goal is not to eliminate luck. Leaf & Let Die is intended to have
uncertainty, swinginess, and drama. The design concern is whether
meaningful decisions remain visible through that randomness.

------------------------------------------------------------------------

## 3. Research program

### 3.1 Dominant purchase paths

Question:

> Can a simple "buy the biggest die possible" or similarly narrow
> economic policy outperform the interesting Plant-card system?

Candidate comparisons:

-   Big-Dice policy vs Human Baseline;
-   Big-Dice policy vs card-focused policy;
-   Big-Dice policy vs tactical Battle policy;
-   Big-Dice policy vs random-legal play;
-   mirror matches.

Repeat across different Groves and Round structures.

### 3.2 Forced-buy cards

For each Plant card, eventually measure quantities such as:

-   availability win delta;
-   forced-priority win delta;
-   first-buy frequency;
-   purchase frequency;
-   activation/use rate;
-   copies owned at end;
-   interaction with other cards/strategies.

Controlled variants should include:

1.  normal availability;
2.  card removed;
3.  one player encouraged/forced to prioritize it;
4.  all players prohibited from buying it.

The existing `CardFocusExperiment` is the first concrete foundation for
this research family.

### 3.3 Trap cards

Look for cards that appear attractive but are systematically poor
choices.

Useful signals include:

-   strong/baseline strategies rarely buy the card;
-   card acquisition correlates with worse outcomes in controlled
    comparisons;
-   the card is bought but rarely activated;
-   opportunity cost consistently exceeds its benefit.

Avoid inferring causation merely from "players who bought this card lost
more." Use paired or forced-priority comparisons where practical.

### 3.4 Strategy diversity

The game should permit multiple viable strategic families rather than
collapsing into one correct script.

Candidate families include:

-   dice-development emphasis;
-   Plant-engine emphasis;
-   Battle/Strike VP emphasis;
-   Wound pressure;
-   Wisp preservation/use;
-   support-resource hoarding;
-   aggressive resource spending;
-   balanced Human Baseline;
-   later tactical/strategic variants.

Important questions:

-   Which strategies have favorable and unfavorable matchups?
-   Does one low-complexity family beat nearly everything?
-   Do multiple families remain viable across random Groves and seats?
-   Does adaptation outperform rigid scripting?

### 3.5 Luck resistance

Measure how sensitive game outcomes are to:

-   dice rolls;
-   Wisp/Critter Roll Rewards;
-   Grove composition;
-   Round Deck order;
-   seat/Battle order;
-   strategic decisions.

Paired seeds and the split mechanical/strategy RNG architecture are
especially important here.

### 3.6 Runaway leader and comeback behavior

At each meaningful checkpoint, record current standing and eventual
result.

Potential metrics:

-   probability that current leader eventually wins;
-   probability that last place eventually wins;
-   comeback rate after each Battle;
-   score-gap distribution by round;
-   remaining meaningful decision opportunities after falling behind.

A leader becoming increasingly likely to win is normal. The warning sign
is when the result is effectively settled long before the game ends.

### 3.7 Wounds

Wounds both damage a Plant Creature and can award bonus VP to the winner
of a Strike. Measure how much of the game they carry.

Potential metrics:

-   win share after suffering the first Wound;
-   win share after causing the first Wound;
-   average Wounds caused/taken;
-   VP attributable to Wound bonuses;
-   frequency with which a Wound changes the eventual winner;
-   comeback rates after being Wounded.

### 3.8 Battle order / column bias

Battle order can be confounded with strong opening dice because the
higher-ranked player is also placed earlier/leftward.

Use mirrored state experiments:

``` text
same Battle state
same dice
same resources
same strategies
swap positions/order
replay
```

Record:

-   column/order position;
-   opening dice strength;
-   Main/Support action order;
-   Strike VP;
-   Wounds;
-   support usage;
-   final game outcome.

The goal is to separate advantage caused by better dice from advantage
caused by acting at a particular time.

### 3.9 Adaptive play versus autopilot

Compare rigid scripts with policies that respond to the actual state.

Example rigid behavior:

-   always buy the most expensive item;
-   always prioritize upgrades;
-   always spend a Bee when it improves a row;
-   always use support immediately.

Adaptive behavior can consider:

-   whether a Strike is already secure;
-   whether a row is too expensive to contest;
-   current Creature/dice development;
-   remaining Battle timing;
-   resource reserves.

A concerning result would be a very simple script performing almost as
well as a strategy that actually evaluates the board.

------------------------------------------------------------------------

## 4. New priority experiment: early-Wisp windfall

The first concrete exploit/luck stress test is now specified precisely
in **Section 5.2, Six-Wisp Opening Stress Test**.

The key question is whether one player receiving three opening rolls of
2 in each of the first two Cultivation rounds --- six early
Wisp-producing rolls --- creates a persistent advantage large enough to
suggest that the game needs an explicit Wisp limit or another balancing
mechanism.

Treat this as a stress test, not as a claim about how often the event
naturally occurs. Use matched controls, rotate the affected-player role
through all four seats, preserve the normal Roll Reward path, and keep
later mechanical randomness paired as closely as possible.

------------------------------------------------------------------------

## 5. Milestone 3 --- experiment harness and first exploit studies

Milestone 2 created the Human Baseline control group. Milestone 3 is
where the simulator begins doing its intended job: **run controlled
complete-game experiments that answer concrete game-design questions**.

### 5.1 Observation architecture: Chronicle → GameSummary → experiment report

A full `GameChronicle` is the detailed history of one game. It is useful
for diagnosis, but it is the wrong object to retain for thousands of
games.

The high-volume path should be:

``` text
run one complete Game
    ↓
GameChronicle contains typed GameEntry history
    ↓
GameSummaryExtractor scans the completed game + Chronicle once
    ↓
produce one compact immutable GameSummary
    ↓
release the Game / Chronicle for garbage collection
    ↓
retain GameSummary
    ↓
after all runs, experiment-specific aggregator/report reads summaries
```

`GameSummary` is the compact per-game research record. It should be
deliberately easy to extend as new questions arise. It must **not**
contain the full Chronicle or mutable `Game` objects.

Initial summary information should include at least:

-   game/seed identity and seat identities;
-   winner IDs and win shares;
-   final VP by player and VP breakdown available from final scoring;
-   Wisp gains, plays/trashes, and unplayed Wisp VP by player;
-   Battle Strike VP by player;
-   Wounds caused/taken;
-   final grafted Plant count and total printed Plant cost;
-   final owned dice count and total die-side power;
-   enough round/checkpoint information to support later comeback
    studies.

Experiment-specific records may wrap or augment `GameSummary` when an
intervention has additional facts, but the detailed Chronicle remains
transient.

This is not merely a memory optimization. It creates the stable boundary
between **what happened in one game** and **what an experiment concludes
across many games**.

**M3-A implementation note.** The first `GameSummary` deliberately records
only metrics that the current typed Chronicle and completed game state can
identify without inference: seeds, seats, winners/win shares, final VP
breakdown, Battle Strike VP, Wounds taken, Wisp Roll Rewards, observable Wisp
plays, final Wisp count, final Plant count/printed cost, and final dice
count/power. The current Chronicle does not identify every Wisp removal source
or an unambiguous player who "caused" every Wound, so M3-A does not invent
those values. Add typed instrumentation when a concrete experiment requires
them. Likewise, round/checkpoint snapshots should be added when the comeback
study is implemented rather than retained speculatively in every summary.

### 5.2 First experiment: Six-Wisp Opening Stress Test

This is the first priority because it answers an immediate rules
question:

> **Does Leaf & Let Die need a hard Wisp limit, or can the existing game
> absorb an extreme early Wisp windfall?**

#### Exact intervention

Use four Human Baseline players.

One role is the **affected player**. During the first two
**Cultivation** rounds only:

-   when that player's Step-2 opening **Draw 3** is rolled;
-   all three of those opening dice are forced to a value of **2**;
-   the normal Roll Reward machinery then resolves those 2s;
-   therefore the intervention attempts to create **three Wisp rewards
    in the first Cultivation opening draw and three more in the
    second**, for six early Wisp-producing rolls total;
-   no later Draw action, reroll, Battle roll, or other die result is
    forced;
-   after the second Cultivation opening Draw 3, all mechanics return
    completely to normal.

Do **not** implement the intervention by directly adding Wisps. The
purpose is to stress the real rules path created by six early rolls of
2, including any consequences that the normal Wisp/reward machinery
causes.

#### Preserve matched randomness

For a matched control/intervention pair, the intervention should still
consume the mechanical RNG result that would normally have been used for
each forced roll and then replace only the observed die face with `2`.
This keeps the later mechanical random stream aligned as closely as
possible.

The intervention mechanism must be explicit and reusable. It should be
scoped by game context (affected player, Cultivation round, and opening
Draw-3 roll) rather than by a fragile rule such as "replace the first
six calls to Randomizer."

Before batch simulation, add a deterministic integration test proving:

1.  only the affected player's three opening dice are forced;
2.  it happens in exactly the first two Cultivation rounds;
3.  each retained forced 2 follows the normal Roll Reward path;
4.  other players are untouched;
5.  later rolls are no longer forced;
6.  the underlying mechanical RNG is still consumed for forced rolls.

#### Initial sample design

The first useful run should be:

``` text
4 Human Baseline players

INTERVENTION:
    affected player receives 3 forced opening 2s
    in each of first 2 Cultivation rounds

2,000 intervention games total
    500 with affected role in seat 1
    500 with affected role in seat 2
    500 with affected role in seat 3
    500 with affected role in seat 4

MATCHED CONTROL:
    2,000 games using the same sample seeds and seat rotations
    but without forced die faces

Total complete game executions: 4,000
```

The affected role is called **Player A** in the report even though that
role rotates through physical seats. This separates "the player
receiving the intervention" from seat/order effects.

Primary result:

> **How much does Player A's win share change relative to the matched
> control?**

Report both **winner rate** and **win share**. Winner rate answers "was
Player A among the winners?" Win share handles ties without counting a
shared win as a full solo win.

Also report:

-   Player A average final VP and matched-control VP delta;
-   six-roll intervention success/count;
-   Player A Wisps gained in the intervention window;
-   total Wisps gained;
-   Wisps played/trashed;
-   unplayed Wisp VP;
-   Wisp Reckoning encounters and losses where observable;
-   Battle Strike VP;
-   Wounds caused/taken;
-   result broken down by physical seat.

Do not decide a rules change from one percentage alone. First establish
the size and consistency of the effect, then inspect representative
Chronicles for reproducible seeds.

### 5.3 Follow-up: Wisp dose-response

If the six-Wisp stress case produces a meaningful advantage, run a
gradient rather than immediately adding a cap:

``` text
control
1 forced early 2
2 forced early 2s
3 forced early 2s
...
6 forced early 2s
```

This distinguishes a normal incremental benefit from a nonlinear
threshold or snowball.

### 5.4 Dominant-minimal-development experiment

The next major exploit family should test whether a deliberately narrow
development plan can dominate richer play.

Example hypothesis:

> A player that builds only a very small Plant Creature of roughly three
> high-value cards and concentrates on a small number of high-sided dice
> may recycle those same strong assets so reliably that the strategy
> wins disproportionately often.

This should be implemented as a **strategy modifier**, not a rigged game
state. Only the Buy/plan preferences needed to pursue the
minimal-development policy should differ; all other decisions should
remain Human Baseline.

The experiment should rotate the modified player through seats and
compare matched seeds against all-Human-Baseline controls.

At minimum report:

-   focused-player win share delta;
-   final VP delta;
-   final Plant count and total printed Plant cost;
-   final dice count and total die-side power;
-   number of high-sided dice;
-   Battle Strike VP;
-   Wounds caused/taken;
-   resource use;
-   whether the strategy's advantage persists across Grove
    configurations.

A red flag is not merely that the strategy can win. The concern is that
a **low-complexity, repeatable, narrow policy** wins so much that
broader development choices become strategically unnecessary.

### 5.5 Card-break experiments

For individual Plant cards or card combinations, use controlled variants
rather than raw correlation.

For a suspicious card:

``` text
CONTROL
    ordinary Human Baseline

FOCUS
    one rotating player intentionally pursues the card

REMOVED
    card unavailable

FORCED / PRIORITIZED
    where useful, force or strongly prioritize acquisition
```

Compare matched seeds and seats. Track acquisition, activation,
survival, VP contribution, Battle contribution, and final outcome. If a
card looks problematic, inspect representative Chronicles and then test
obvious counters before changing the card.

### 5.6 Milestone-3 implementation order

Implement the research harness in this order:

1.  **M3-A --- compact per-game summary --- IMPLEMENTED, pending focused test**
    -   `GameSummary` / `PlayerGameSummary` are compact immutable value records;
    -   `GameSummaryExtractor` collapses a completed `Game` + typed Chronicle;
    -   extraction includes only metrics supported reliably by current typed data;
    -   focused simulation coverage runs a real completed Human Baseline game;
    -   neither summary type retains the full Chronicle or mutable `Game`.
2.  **M3-B --- reusable experiment batch/report boundary --- IMPLEMENTED, pending focused test**
    -   `GameSummaryBatchRunner` runs isolated complete games and retains only `GameSummary` values;
    -   deterministic per-sample mechanical and strategy seed schedules support matched runs;
    -   `Matchup.seatRotations()` supplies cyclic role/strategy rotation across physical seats;
    -   `BatchRunResult` is the compact handoff to experiment-specific aggregation;
    -   `BatchReport` supplies a small generic text view and one-row-per-player CSV export without becoming a general analytics framework;
    -   decision reasoning is disabled and completed `Game`/Chronicle objects are not retained by the batch result;
    -   concrete experiments remain responsible for their own intervention metadata, paired deltas, and specialized reports.
3.  **M3-C --- controlled mechanical intervention seam**
    -   add a reusable way to alter selected mechanical outcomes for
        experiments;
    -   preserve normal engine rule execution;
    -   preserve later RNG alignment by consuming displaced natural
        results;
    -   keep intervention logic out of normal game strategy.
4.  **M3-D --- Six-Wisp integration proof**
    -   deterministic real-engine test of the exact
        first-two-Cultivation opening intervention;
    -   prove six forced 2s occur only where intended and rewards use
        the normal path.
5.  **M3-E --- Six-Wisp batch experiment**
    -   2,000 intervention games + 2,000 matched controls;
    -   500 affected-role games per seat;
    -   Human Baseline for all players;
    -   aggregate/report from compact summaries.
6.  **M3-F --- interpret and diagnose**
    -   identify effect size and seat consistency;
    -   rerun representative seeds with full Chronicle;
    -   decide whether a dose-response study is warranted;
    -   do not change game rules merely because the intervention helps.
7.  **M3-G --- first strategy exploit experiment**
    -   implement the minimal-Plant/high-dice strategy modifier;
    -   matched Human Baseline control;
    -   rotate seats and report the same core development/outcome
        metrics.
8.  **M3-H --- card-break experiment family**
    -   use the existing card-focus foundation;
    -   add remove/prioritize variants as actual questions require.

The guiding rule for Milestone 3 is:

> **The simulator should make a design claim testable: define one
> intervention or strategy difference, preserve everything else as much
> as possible, run enough complete games, collapse each game to a
> compact summary, and report the resulting delta.**

------------------------------------------------------------------------

## 6. Reporting goals

As experiment coverage grows, the simulator should be able to produce
recurring reports.

### Balance report

-   win share by strategy;
-   average final VP;
-   VP by source;
-   Wounds caused/taken;
-   Plant cards bought;
-   dice gained/upgraded;
-   Wisps gained/played/retained.

### Strategy report

-   matchup matrix;
-   favorable/unfavorable matchups;
-   strategy performance across Groves;
-   performance by player count;
-   performance by seat;
-   strategy-complexity versus performance.

### Card report

-   availability delta;
-   acquisition/purchase rate;
-   first-buy frequency;
-   activations;
-   final copies;
-   VP/effect contribution;
-   cards consistently avoided or consistently prioritized.

### Luck-resistance report

-   paired-strategy results with fixed mechanical seeds;
-   mechanical-seed sensitivity;
-   seat/order sensitivity;
-   Grove/Round sensitivity;
-   comeback rate;
-   winner predictability by round.

### Exploit report

-   low-complexity strategies with unusually high performance;
-   forced-buy behavior;
-   abnormal card/combo results;
-   interventions with large persistent win-share deltas;
-   strategies that remain strong after obvious counters are introduced.

------------------------------------------------------------------------

## 7. Development stages

The project should continue to grow in stages rather than jumping
directly to sophisticated AI.

### Stage A --- correct and observable engine

-   legal action generation;
-   exact state transitions;
-   typed Chronicle;
-   deterministic scenario control;
-   snapshots/assertions.

Much of this foundation now exists.

### Stage B --- meaningful Human Baseline

Create a simple but realistic control strategy. Its purpose is not
optimality; its purpose is to avoid obviously irrational behavior that
would make simulation conclusions misleading.

The current baseline architecture already contains shared scoring
primitives, shared heuristics, card-specific priorities, and
strategy-only tie randomness.

### Stage C --- focused experiments

Build narrow, interpretable experiments first:

-   focused card acquisition;
-   early-Wisp windfall;
-   big-dice purchase pressure;
-   Battle-order mirroring;
-   Wound leverage;
-   resource-hoarding versus spending.

### Stage D --- general batch/tournament reporting

Expand the generic `BatchRunner`/`Matchup`/`Tournament` boundary into
practical high-volume runners and common reports.

### Stage E --- tactical search

Search is most promising first in constrained Battle windows where legal
actions and outcomes are tractable and the result is easy to interpret.

### Stage F --- learned/adaptive strategies

Only after the simpler research framework is producing useful evidence
should learning systems become a major focus. Interpretability remains
more valuable for game design than raw playing strength.

------------------------------------------------------------------------

## 8. Near-term experiment backlog

A practical sequence is:

1.  Treat Human Baseline as the stable control strategy.
2.  Build the compact Chronicle → `GameSummary` observation boundary.
3.  Finish the reusable paired batch/report runner.
4.  Add the reusable controlled-mechanical-intervention seam.
5.  Run the **Six-Wisp Opening Stress Test**: 2,000 intervention games
    plus 2,000 matched controls.
6.  If warranted, run the early-Wisp dose-response series.
7.  Test the minimal-Plant/high-dice strategy for dominance.
8.  Expand focused-card experiments into remove/prioritize/card-combo
    studies.
9.  Add mirrored Battle-order, Wound leverage, comeback, and matchup
    experiments.
10. Only then decide which questions actually require Tactical search or
    learned/adaptive play.

The core principle throughout is: **change one thing, control what you
can, rotate confounding positions, run enough games, and record why the
outcome changed.**
